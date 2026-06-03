package turtleprov

import rdfkt.Literal
import rdfkt.NamedTerm
import rdfkt.XSD
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

        assertEquals(1, store.quads.size)
        assertEquals("http://example.com/", store.prefixes["ex"])

        val provQuad = store.quads.first()
        assertEquals(NamedTerm("http://example.com/alice"), provQuad.quad.s)
        assertEquals(NamedTerm("http://example.com/knows"), provQuad.quad.p)
        assertEquals(NamedTerm("http://example.com/bob"), provQuad.quad.o)

        assertNotNull(provQuad.subjectInfo)
        assertNotNull(provQuad.predicateInfo)
        assertNotNull(provQuad.objectInfo)
    }

    @Test
    fun `parses typed literal`() {
        val turtle = """
            @prefix ex: <http://example.com/> .
            ex:alice ex:age 42 .
        """.trimIndent()

        val store = parseTurtleFromString(turtle)

        assertEquals(1, store.quads.size)
        val obj = store.quads.first().quad.o
        assertTrue(obj is Literal)
        assertEquals("42", obj.value)
        assertEquals(XSD.int.uri, obj.type?.uri)
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
        assertTrue(store.quads.size >= 8)

        assertTrue(
            store.quads.any { provQuad ->
                provQuad.quad.s == NamedTerm("http://example.com/base/TriplesMap1") &&
                        provQuad.quad.p == NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") &&
                        provQuad.quad.o == NamedTerm("http://w3id.org/rml/TriplesMap")
            }
        )
    }

    private fun parseSingleLiteral(
        predicateLocalName: String,
        objectSyntax: String
    ): Triple<Literal, ProvQuad, String> {
        val turtle = listOf(
            "@prefix ex: <http://example.com/> .",
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
            "ex:s ex:$predicateLocalName $objectSyntax ."
        ).joinToString("\n")

        val store = parseTurtleFromString(turtle)
        val provQuad = store.quads.first { it.quad.p == NamedTerm("http://example.com/$predicateLocalName") }
        val literal = provQuad.quad.o as Literal
        return Triple(literal, provQuad, turtle.lines()[2])
    }

    private fun assertLiteralObjectProvenance(
        provQuad: ProvQuad,
        line: String,
        expectedKind: TurtleNodeKind,
        expectedLiteralToken: String,
        expectedLiteralContent: String,
    ) {
        val objectInfo = assertNotNull(provQuad.objectInfo)
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
        val (literal, provQuad, line) = parseSingleLiteral("plain", "\"double\"")
        assertEquals("double", literal.value)
        assertEquals(XSD.string.uri, literal.type?.uri)
        assertLiteralObjectProvenance(
            provQuad,
            line,
            TurtleNodeKind.STRING_LITERAL_QUOTE,
            "\"double\"",
            "double"
        )
    }

    @Test
    fun `parses rdf literal with single quotes and provenance spans`() {
        val (literal, provQuad, line) = parseSingleLiteral("single", "'single'")
        assertEquals("single", literal.value)
        assertEquals(XSD.string.uri, literal.type?.uri)
        assertLiteralObjectProvenance(
            provQuad,
            line,
            TurtleNodeKind.STRING_LITERAL_SINGLE_QUOTE,
            "'single'",
            "single"
        )
    }

    @Test
    fun `parses rdf literal with long double quotes and provenance spans`() {
        val (literal, provQuad, line) = parseSingleLiteral("longDouble", "\"\"\"long double\"\"\"")
        assertEquals("long double", literal.value)
        assertEquals(XSD.string.uri, literal.type?.uri)
        assertLiteralObjectProvenance(
            provQuad,
            line,
            TurtleNodeKind.STRING_LITERAL_LONG_QUOTE,
            "\"\"\"long double\"\"\"",
            "long double"
        )
    }

    @Test
    fun `parses rdf literal with long single quotes and provenance spans`() {
        val (literal, provQuad, line) = parseSingleLiteral("longSingle", "'''long single'''")
        assertEquals("long single", literal.value)
        assertEquals(XSD.string.uri, literal.type?.uri)
        assertLiteralObjectProvenance(
            provQuad,
            line,
            TurtleNodeKind.STRING_LITERAL_LONG_SINGLE_QUOTE,
            "'''long single'''",
            "long single"
        )
    }

    @Test
    fun `parses rdf literal with language tag and provenance spans`() {
        val (literal, provQuad, line) = parseSingleLiteral("lang", "\"Bonjour\"@fr")
        assertEquals("Bonjour", literal.value)
        assertEquals("fr", literal.lang)
        assertLiteralObjectProvenance(
            provQuad,
            line,
            TurtleNodeKind.STRING_LITERAL_QUOTE,
            "\"Bonjour\"@fr",
            "Bonjour"
        )
    }

    @Test
    fun `parses rdf literal with datatype and provenance spans`() {
        val (literal, provQuad, line) = parseSingleLiteral("typed", "\"42\"^^xsd:integer")
        assertEquals("42", literal.value)
        assertEquals("http://www.w3.org/2001/XMLSchema#integer", literal.type?.uri)
        assertLiteralObjectProvenance(
            provQuad,
            line,
            TurtleNodeKind.STRING_LITERAL_QUOTE,
            "\"42\"^^xsd:integer",
            "42"
        )
    }
}