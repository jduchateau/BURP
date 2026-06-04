package rdfobjectloader

import burp.model.*
import burp.parse.ParseCodegen
import java.nio.file.Paths
import kotlin.test.*

class ParseRMLFNMLTC0002CSV {

    @Test
    fun testParseFunctionExecution() {
        val mappingPath = Paths.get("src/test/resources/rml-fnml/RMLFNMLTC0002-CSV/mapping.ttl")
        val currentDirectory = Paths.get("src/test/resources/rml-fnml/RMLFNMLTC0002-CSV")

        val triplesMaps = ParseCodegen().parseMappingFile(mappingPath, currentDirectory)

        assertEquals(1, triplesMaps.size)
        val triplesMap = triplesMaps.first()

        // Assert Logical Source
        val logicalSource = triplesMap.logicalSource
        assertNotNull(logicalSource)
        assertIs<burp.ls.CSVSource>(logicalSource)
        assertEquals("http://w3id.org/rml/CSV", logicalSource.referenceFormulation.value)

        val file = logicalSource.file
        assertIs<burp.ls.SourceFile.Local>(file)
        val expectedPath = mappingPath.toAbsolutePath().normalize().parent.resolve("student.csv").toString()
        assertEquals(expectedPath, file.path)

        // Assert Subject Map
        val subjectMap = triplesMap.subjectMap
        assertNotNull(subjectMap)
        val subjectExpr = subjectMap.expression
        assertIs<Template>(subjectExpr)
        assertEquals("http://example.com/{Name}", subjectExpr.template)

        // Assert Predicate Object Map
        assertEquals(1, triplesMap.predicateObjectMaps.size)
        val pom = triplesMap.predicateObjectMaps.first()

        assertTrue(pom.predicateMaps.isNotEmpty())
        val predMap = pom.predicateMaps.first()
        val predExpr = predMap.expression
        assertIs<RDFNodeConstant>(predExpr)
        assertEquals("http://xmlns.com/foaf/0.1/name", predExpr.constant?.value)

        assertEquals(1, pom.objectMaps.size)
        val objMap = pom.objectMaps.first()
        assertIs<ObjectMap>(objMap)
        val objExpr = objMap.expression
        assertIs<FunctionExecution>(objExpr)

        val funcExpr = objExpr.functionMap?.expression
        assertIs<RDFNodeConstant>(funcExpr)
        assertEquals("http://users.ugent.be/~bjdmeest/function/grel.ttl#toUpperCase", funcExpr.constant?.value)
        
        kotlin.test.assertNull(objExpr.returnMap)

        assertEquals(1, objExpr.inputs.size)
        val input = objExpr.inputs.first()
        
        val paramExpr = input.parameterMap.expression
        assertIs<RDFNodeConstant>(paramExpr)
        assertEquals("http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam", paramExpr.constant?.value)
        
        val valInputExpr = input.inputValueMap.expression
        assertIs<RawReference>(valInputExpr)
        assertEquals("Name", valInputExpr.reference)
    }
}
