package rdfobjectloader

import burp.model.FunctionExecution
import burp.model.RDFNodeConstant
import burp.model.RawReference
import burp.model.Template
import burp.parse.ParseCodegen
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ParseRMLFNMLTC0032CSV {

    @Test
    fun testParseConditionRdfTerm() {
        val mappingPath = Paths.get("src/test/resources/rml-fnml/RMLFNMLTC0032-CSV/mapping.ttl")
        val currentDirectory = Paths.get("src/test/resources/rml-fnml/RMLFNMLTC0032-CSV")

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

        // Assert Subject Map expression is idlab-fn:IF function execution
        val subjectMap = triplesMap.subjectMap
        assertNotNull(subjectMap)
        val expression = subjectMap.expression
        assertIs<FunctionExecution>(expression)

        val functionMapExpr = expression.functionMap?.expression
        assertIs<RDFNodeConstant>(functionMapExpr)
        assertEquals("https://w3id.org/imec/idlab/function#IF", functionMapExpr.constant?.value)

        // Inputs for idlab-fn:IF
        assertEquals(2, expression.inputs.size)

        // 1. Boolean parameter (the condition: idlab-fn:equal)
        val boolInput = expression.inputs.find {
            val paramExpr = it.parameterMap.expression
            assertIs<RDFNodeConstant>(paramExpr)
            paramExpr.constant?.value == "https://w3id.org/imec/idlab/function#boolParameter"
        }
        assertNotNull(boolInput)
        val boolExpr = boolInput.inputValueMap.expression
        assertIs<FunctionExecution>(boolExpr)
        
        val boolFuncExpr = boolExpr.functionMap?.expression
        assertIs<RDFNodeConstant>(boolFuncExpr)
        assertEquals("https://w3id.org/imec/idlab/function#equal", boolFuncExpr.constant?.value)
        
        kotlin.test.assertNull(boolExpr.returnMap)

        // Equal function inputs: grel:valueParam (Class reference) and grel:valueParam2 (constant "A")
        assertEquals(2, boolExpr.inputs.size)
        val valueParamInput = boolExpr.inputs.find {
            val paramExpr = it.parameterMap.expression
            assertIs<RDFNodeConstant>(paramExpr)
            paramExpr.constant?.value == "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        }
        assertNotNull(valueParamInput)
        val valInputExpr = valueParamInput.inputValueMap.expression
        assertIs<RawReference>(valInputExpr)
        assertEquals("Class", valInputExpr.reference)

        val valueParam2Input = boolExpr.inputs.find {
            val paramExpr = it.parameterMap.expression
            assertIs<RDFNodeConstant>(paramExpr)
            paramExpr.constant?.value == "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam2"
        }
        assertNotNull(valueParam2Input)
        val val2InputExpr = valueParam2Input.inputValueMap.expression
        assertIs<RDFNodeConstant>(val2InputExpr)
        assertEquals("A", val2InputExpr.constant?.value)

        // 2. Expression parameter (the template "http://example.com/{Name}")
        val exprInput = expression.inputs.find {
            val paramExpr = it.parameterMap.expression
            assertIs<RDFNodeConstant>(paramExpr)
            paramExpr.constant?.value == "https://w3id.org/imec/idlab/function#expressionParameter"
        }
        assertNotNull(exprInput)
        val exprInputVal = exprInput.inputValueMap.expression
        assertIs<Template>(exprInputVal)
        assertEquals("http://example.com/{Name}", exprInputVal.template)
    }
}
