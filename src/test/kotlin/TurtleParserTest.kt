package turtleprov

import org.apache.jena.rdf.model.Literal
import org.apache.jena.vocabulary.XSD
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TurtleParserTest {

    @Test
    fun `parses a simple turtle triple with provenance`() {
        val turtle = """
            @prefix ex: <http://example.com/> .
            ex:alice ex:knows ex:bob .
        """.trimIndent()

        val store = parseTurtleFromString(turtle)

        assertEquals(1, store.triples.size)
        assertEquals("http://example.com/", store.prefixes["ex"])

        val provTriple = store.triples.first()
        assertEquals("http://example.com/alice", provTriple.statement.subject.uri)
        assertEquals("http://example.com/knows", provTriple.statement.predicate.uri)
        assertEquals("http://example.com/bob", provTriple.statement.`object`.asResource().uri)

        assertNotNull(provTriple.subjectInfo)
        assertNotNull(provTriple.predicateInfo)
        assertNotNull(provTriple.objectInfo)
    }

    @Test
    fun `parses typed literal`() {
        val turtle = """
            @prefix ex: <http://example.com/> .
            ex:alice ex:age 42 .
        """.trimIndent()

        val store = parseTurtleFromString(turtle)

        assertEquals(1, store.triples.size)
        val obj = store.triples.first().statement.`object`
        assertTrue(obj.isLiteral)
        val literal = obj.asLiteral()
        assertEquals("42", literal.lexicalForm)
        assertEquals("http://www.w3.org/2001/XMLSchema#int", literal.datatypeURI)
    }

    @Test
    fun `parses RMLTC0000 JSON fixture in common test`() {
        val fixture = """
            @prefix foaf: <http://xmlns.com/foaf/0.1/> .
            @prefix rml: <http://w3id.org/rml/> .

            <http://example.com/base/TriplesMap1> a rml:TriplesMap;
              rml:logicalSource [ a rml:LogicalSource;
                  rml:iterator "$.students[*]";
                  rml:referenceFormulation rml:JSONPath;
                  rml:source [ a rml:RelativePathSource;
                      rml:root rml:MappingDirectory;
                      rml:path "student.json"
                    ]
                ];
              rml:predicateObjectMap [
                  rml:objectMap [
                      rml:reference "$.Name"
                    ];
                  rml:predicate foaf:name
                ];
              rml:subjectMap [
                  rml:template "http://example.com/{$.Name}"
                ] .
        """.trimIndent()

        val store = parseTurtleFromString(fixture)

        assertEquals("http://xmlns.com/foaf/0.1/", store.prefixes["foaf"])
        assertEquals("http://w3id.org/rml/", store.prefixes["rml"])
        assertTrue(store.triples.size >= 8)

        assertTrue(
            store.triples.any { provTriple ->
                provTriple.statement.subject.uri == "http://example.com/base/TriplesMap1" &&
                        provTriple.statement.predicate.uri == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" &&
                        provTriple.statement.`object`.isResource &&
                        provTriple.statement.`object`.asResource().uri == "http://w3id.org/rml/TriplesMap"
            }
        )
    }

    private fun parseSingleLiteral(
        predicateLocalName: String,
        objectSyntax: String
    ): Triple<Literal, ProvTriple, String> {
        val turtle = listOf(
            "@prefix ex: <http://example.com/> .",
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
            "ex:s ex:$predicateLocalName $objectSyntax ."
        ).joinToString("\n")

        val store = parseTurtleFromString(turtle)
        val provTriple = store.triples.first { it.statement.predicate.uri == "http://example.com/$predicateLocalName" }
        val literal = provTriple.statement.`object`.asLiteral()
        return Triple(literal, provTriple, turtle.lines()[2])
    }

    private fun assertLiteralObjectProvenance(
        provTriple: ProvTriple,
        line: String,
        expectedKind: TurtleNodeKind,
        expectedLiteralToken: String,
        expectedLiteralContent: String,
    ) {
        val objectInfo = assertNotNull(provTriple.objectInfo)
        val start = assertNotNull(objectInfo.start)
        val end = assertNotNull(objectInfo.end)

        assertEquals(expectedKind, objectInfo.kind)
        assertEquals(2, start.line)
        assertEquals(2, end.line)
        assertTrue(start.column <= end.column)
        assertEquals(expectedLiteralToken, line.substring(start.column, end.column))

        val stringStart = assertNotNull(objectInfo.rdfLiteralStringStart)
        val stringEnd = assertNotNull(objectInfo.rdfLiteralStringEnd)
        assertEquals(2, stringStart.line)
        assertEquals(2, stringEnd.line)
        assertTrue(stringStart.column <= stringEnd.column)
        assertEquals(expectedLiteralContent, line.substring(stringStart.column, stringEnd.column))
    }

    @Test
    fun `parses rdf literal with double quotes and provenance spans`() {
        val (literal, provTriple, line) = parseSingleLiteral("plain", "\"double\"")
        assertEquals("double", literal.lexicalForm)
        assertEquals(XSD.xstring.uri, literal.datatypeURI)
        assertLiteralObjectProvenance(
            provTriple,
            line,
            TurtleNodeKind.STRING_LITERAL_QUOTE,
            "\"double\"",
            "double"
        )
    }

    @Test
    fun `parses rdf literal with single quotes and provenance spans`() {
        val (literal, provTriple, line) = parseSingleLiteral("single", "'single'")
        assertEquals("single", literal.lexicalForm)
        assertEquals(XSD.xstring.uri, literal.datatypeURI)
        assertLiteralObjectProvenance(
            provTriple,
            line,
            TurtleNodeKind.STRING_LITERAL_SINGLE_QUOTE,
            "'single'",
            "single"
        )
    }

    @Test
    fun `parses rdf literal with long double quotes and provenance spans`() {
        val (literal, provTriple, line) = parseSingleLiteral("longDouble", "\"\"\"long double\"\"\"")
        assertEquals("long double", literal.lexicalForm)
        assertEquals(XSD.xstring.uri, literal.datatypeURI)
        assertLiteralObjectProvenance(
            provTriple,
            line,
            TurtleNodeKind.STRING_LITERAL_LONG_QUOTE,
            "\"\"\"long double\"\"\"",
            "long double"
        )
    }

    @Test
    fun `parses rdf literal with long single quotes and provenance spans`() {
        val (literal, provTriple, line) = parseSingleLiteral("longSingle", "'''long single'''")
        assertEquals("long single", literal.lexicalForm)
        assertEquals(XSD.xstring.uri, literal.datatypeURI)
        assertLiteralObjectProvenance(
            provTriple,
            line,
            TurtleNodeKind.STRING_LITERAL_LONG_SINGLE_QUOTE,
            "'''long single'''",
            "long single"
        )
    }

    @Test
    fun `parses rdf literal with language tag and provenance spans`() {
        val (literal, provTriple, line) = parseSingleLiteral("lang", "\"Bonjour\"@fr")
        assertEquals("Bonjour", literal.lexicalForm)
        assertEquals("fr", literal.language)
        assertLiteralObjectProvenance(
            provTriple,
            line,
            TurtleNodeKind.STRING_LITERAL_QUOTE,
            "\"Bonjour\"@fr",
            "Bonjour"
        )
    }

    @Test
    fun `parses rdf literal with datatype and provenance spans`() {
        val (literal, provTriple, line) = parseSingleLiteral("typed", "\"42\"^^xsd:integer")
        assertEquals("42", literal.lexicalForm)
        assertEquals("http://www.w3.org/2001/XMLSchema#integer", literal.datatypeURI)
        assertLiteralObjectProvenance(
            provTriple,
            line,
            TurtleNodeKind.STRING_LITERAL_QUOTE,
            "\"42\"^^xsd:integer",
            "42"
        )
    }
}