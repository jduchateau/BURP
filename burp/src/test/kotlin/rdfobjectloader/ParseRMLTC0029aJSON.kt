package rdfobjectloader

import burp.ls.JSONSourceRFC
import burp.ls.SourceFile
import burp.model.ObjectMap
import burp.model.RDFNodeConstant
import burp.model.Reference
import burp.parse.ParseCodegen
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ParseRMLTC0029aJSON {

    @Test
    fun testParseConstantRdfTerm() {
        val mappingPath = Paths.get("src/test/resources/rml-core/RMLTC0029a-JSON/mapping.ttl")
        val currentDirectory = Paths.get("src/test/resources/rml-core/RMLTC0029a-JSON")

        val triplesMaps = ParseCodegen().parseMappingFile(mappingPath, currentDirectory)

        assertEquals(1, triplesMaps.size)
        val triplesMap = triplesMaps.first()

        val logicalSource = triplesMap.logicalSource
        assertNotNull(logicalSource)
        assertIs<JSONSourceRFC>(logicalSource)
        assertEquals("$[*]", logicalSource.iterator)
        assertEquals("http://w3id.org/rml/JSONPath", logicalSource.referenceFormulation.value)

        val file = logicalSource.file
        assertIs<SourceFile.Local>(file)
        val expectedPath = mappingPath.toAbsolutePath().normalize().parent.resolve("data.json").toString()
        assertEquals(expectedPath, file.path)

        assertNotNull(triplesMap.subjectMap)
        val expression = triplesMap.subjectMap.expression
        assertNotNull(expression)
        assertIs<RDFNodeConstant>(expression)
        assertEquals("http://example.com/example", expression.constant?.value)

        assertEquals(1, triplesMap.predicateObjectMaps.size)
        val pom = triplesMap.predicateObjectMaps.first()
        kotlin.test.assertTrue(pom.predicateMaps.isNotEmpty())
        val predicateMap = pom.predicateMaps.first()
        assertIs<RDFNodeConstant>( predicateMap.expression)
        assertEquals("http://example.com/x", (predicateMap.expression as RDFNodeConstant).constant?.value)

        assertEquals(1, pom.objectMaps.size)
        val objectMap = pom.objectMaps.first()
        assertIs<ObjectMap>(objectMap)
        assertIs<Reference>(objectMap.expression)
        assertEquals("$.FOO", (objectMap.expression as Reference).reference)
    }
}