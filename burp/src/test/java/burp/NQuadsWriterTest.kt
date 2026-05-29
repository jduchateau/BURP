package burp

import burp.model.BlankNodeTerm
import burp.model.IRITerm
import burp.model.LiteralTerm
import burp.model.RdfStatement
import burp.vocabularies.RML
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class NQuadsWriterTest {

    @Test
    fun writesUnsafeIriAndEscapedLiterals() {
        val statements = listOf(
            RdfStatement(
                subject = IRITerm("http://example.org/unsafe subject"),
                predicate = IRITerm("http://example.org/p"),
                `object` = IRITerm("http://example.org/unsafe object"),
                graph = IRITerm("http://example.org/unsafe graph")
            ),
            RdfStatement(
                subject = BlankNodeTerm("node-1"),
                predicate = IRITerm("http://example.org/label"),
                `object` = LiteralTerm("line1\n\"quoted\"\\slash", language = "en"),
                graph = IRITerm(RML.defaultGraph.uri)
            )
        )

        val output = ByteArrayOutputStream()
        NQuadsWriter.write(output, statements)

        val serialized = output.toString(StandardCharsets.UTF_8)
        val expected = "<http://example.org/unsafe subject> <http://example.org/p> <http://example.org/unsafe object> <http://example.org/unsafe graph> .\n" +
            "_:node-1 <http://example.org/label> \"line1\\n\\\"quoted\\\"\\\\slash\"@en .\n"

        assertEquals(expected, serialized)
    }

    @Test
    fun omitsDefaultGraphFromQuadLine() {
        val statement = RdfStatement(
            subject = IRITerm("http://example.org/s"),
            predicate = IRITerm("http://example.org/p"),
            `object` = LiteralTerm("value", datatype = IRITerm("http://example.org/type")),
            graph = IRITerm(RML.defaultGraph.uri)
        )

        val serialized = NQuadsWriter.serializeStatement(statement)

        assertEquals(
            "<http://example.org/s> <http://example.org/p> \"value\"^^<http://example.org/type> .",
            serialized
        )
    }


    @Test
    fun writeDatatypes() {
        val statement = RdfStatement(
            subject = IRITerm("http://example.org/s"),
            predicate = IRITerm("http://example.org/p"),
            `object` = LiteralTerm("100", datatype = IRITerm("http://www.w3.org/2001/XMLSchema#double"))
        )

        val serialized = NQuadsWriter.serializeStatement(statement)

        assertEquals(
            "<http://example.org/s> <http://example.org/p> \"100\"^^<http://www.w3.org/2001/XMLSchema#double> .",
            serialized
        )
    }

    @Test
    fun writeLanguageTag() {
        val statement = RdfStatement(
            subject = IRITerm("http://example.org/s"),
            predicate = IRITerm("http://example.org/p"),
            `object` = LiteralTerm("value", language = "en-GB")
        )

        val serialized = NQuadsWriter.serializeStatement(statement)

        assertEquals(
            "<http://example.org/s> <http://example.org/p> \"value\"@en-GB .",
            serialized
        )
    }


    @Test
    fun omitsExplicitXsdStringDatatypeInLiteralSerialization() {
        val statement = RdfStatement(
            subject = IRITerm("http://example.org/s"),
            predicate = IRITerm("http://example.org/p"),
            `object` = LiteralTerm("value", datatype = IRITerm("http://www.w3.org/2001/XMLSchema#string"))
        )

        val serialized = NQuadsWriter.serializeStatement(statement)

        assertEquals(
            "<http://example.org/s> <http://example.org/p> \"value\" .",
            serialized
        )
    }

    @Test
    fun keepsPlainLiteralSerializationUnchanged() {
        val statement = RdfStatement(
            subject = IRITerm("http://example.org/s"),
            predicate = IRITerm("http://example.org/p"),
            `object` = LiteralTerm("value")
        )

        val serialized = NQuadsWriter.serializeStatement(statement)

        assertEquals(
            "<http://example.org/s> <http://example.org/p> \"value\" .",
            serialized
        )
    }
}



