package burp.parse

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParseTest {

    @Test
    fun testExtractStatementsFromShaclViolation() {
        val shapesGraph = """
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix ex: <http://example.org/> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

            ex:PersonShape
                a sh:NodeShape ;
                sh:targetClass ex:Person ;
                sh:property [
                    sh:path ex:age ;
                    sh:datatype xsd:integer ;
                ] .
        """.trimIndent()

        val dataGraph = """
            @prefix ex: <http://example.org/> .
            
            ex:Alice a ex:Person ;
                ex:age "twenty" .
        """.trimIndent()

        val shapesModel = ModelFactory.createDefaultModel()
        shapesModel.read(shapesGraph.byteInputStream(), null, "TURTLE")
        
        val dataModel = ModelFactory.createDefaultModel()
        dataModel.read(dataGraph.byteInputStream(), null, "TURTLE")

        val report = ShaclValidator.get().validate(shapesModel.graph, dataModel.graph)
        val entries = report.entries
        
        assertEquals(1, entries.size, "Should have 1 violation")
        val vr = entries.iterator().next()
        
        val parse = Parse()
        val statements = parse.extractStatementsFromShaclViolation(vr, dataModel)
        
        assertEquals(1, statements.size, "Should extract 1 relevant statement")
        assertEquals("http://example.org/Alice", statements[0].stmt.subject.uri)
        assertEquals("http://example.org/age", statements[0].stmt.predicate.uri)
        assertEquals("twenty", statements[0].stmt.getObject().asLiteral().lexicalForm)
    }

    @Test
    fun testExtractStatementsFromShaclViolation_minCount() {
        val shapesGraph = """
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix ex: <http://example.org/> .

            ex:PersonShape
                a sh:NodeShape ;
                sh:targetClass ex:Person ;
                sh:property [
                    sh:path ex:name ;
                    sh:minCount 1 ;
                ] .
        """.trimIndent()

        val dataGraph = """
            @prefix ex: <http://example.org/> .
            ex:Bob a ex:Person .
        """.trimIndent()

        val shapesModel = ModelFactory.createDefaultModel()
        shapesModel.read(shapesGraph.byteInputStream(), null, "TURTLE")
        
        val dataModel = ModelFactory.createDefaultModel()
        dataModel.read(dataGraph.byteInputStream(), null, "TURTLE")

        val report = ShaclValidator.get().validate(shapesModel.graph, dataModel.graph)
        val entries = report.entries
        
        assertEquals(1, entries.size, "Should have 1 violation")
        val vr = entries.iterator().next()
        
        val parse = Parse()
        val statements = parse.extractStatementsFromShaclViolation(vr, dataModel)
        
        // In minCount, it looks for the statements with ex:name, but Bob has none.
        // It should fallback to retrieving statements where Bob is involved.
        assertEquals(1, statements.size, "Should fallback to focus nodes statement")
        assertEquals("http://example.org/Bob", statements[0].stmt.subject.uri)
        assertEquals(RDF.type.uri, statements[0].stmt.predicate.uri)
        assertEquals("http://example.org/Person", statements[0].stmt.getObject().asResource().uri)
    }
}
