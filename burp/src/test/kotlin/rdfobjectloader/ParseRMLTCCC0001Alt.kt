package rdfobjectloader

import burp.ls.JSONSourceRFC
import burp.ls.SourceFile
import burp.model.*
import burp.parse.ParseCodegen
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ParseRMLTCCC0001Alt {

    @Test
    fun testParseGatherMap() {
        val mappingPath = Paths.get("src/test/resources/rml-cc/RMLTC-CC-0001-Alt/mapping.ttl")
        val currentDirectory = Paths.get("src/test/resources/rml-cc/RMLTC-CC-0001-Alt")

        val triplesMaps = ParseCodegen().parseMappingFile(mappingPath, currentDirectory)

        assertEquals(1, triplesMaps.size)
        val triplesMap = triplesMaps.first()

        // Assert Logical Source
        val logicalSource = triplesMap.logicalSource
        assertNotNull(logicalSource)
        assertIs<JSONSourceRFC>(logicalSource)
        assertEquals("http://w3id.org/rml/JSONPath", logicalSource.referenceFormulation.value)
        assertEquals("$.*", logicalSource.iterator)

        val file = logicalSource.file
        assertIs<SourceFile.Local>(file)
        val expectedPath = mappingPath.toAbsolutePath().normalize().parent.resolve("data.json").toString()
        assertEquals(expectedPath, file.path)

        // Assert Subject Map
        val subjectMap = triplesMap.subjectMap
        assertNotNull(subjectMap)
        val subjectExpr = subjectMap.expression
        assertIs<Template>(subjectExpr)
        assertEquals("e/{$.id}", subjectExpr.template)

        // Assert Predicate Object Map
        assertEquals(1, triplesMap.predicateObjectMaps.size)
        val pom = triplesMap.predicateObjectMaps.first()
        
        kotlin.test.assertTrue(pom.predicateMaps.size >= 1)
        val predMap = pom.predicateMaps.first()
        val predExpr = predMap.expression
        assertIs<RDFNodeConstant>(predExpr)
        assertEquals("http://example.com/ns#with", predExpr.constant?.value)

        assertEquals(1, pom.objectMaps.size)
        val objMap = pom.objectMaps.first()
        assertIs<ObjectMap>(objMap)
        
        // Assert Gather Map
        val gatherMap = objMap.gatherMap
        assertNotNull(gatherMap)
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt", gatherMap.gatherAs?.value)
        
        assertEquals(1, gatherMap.gatherMaps.size)
        val gatheredTermGenerator = gatherMap.gatherMaps.first()
        assertIs<TermMap>(gatheredTermGenerator)
        
        val gatheredExpr = gatheredTermGenerator.expression
        assertIs<RawReference>(gatheredExpr)
        assertEquals("$.values.*", gatheredExpr.reference)
    }
}
