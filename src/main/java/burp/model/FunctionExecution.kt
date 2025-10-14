package burp.model

import burp.errors.BurpException
import burp.model.fnmlutil.FunctionsRegistry
import burp.vocabularies.RMLError

class FunctionExecution : Expression() {
    var functionMap: FunctionMap? = null
    var inputs: MutableList<Input> = ArrayList<Input>()
    var returnMap: ReturnMap? = null

    private fun throwExactlyOneValueError(mapName: String): Nothing {
        throw BurpException(
            RMLError.OutOfSpec,
            "$mapName should generate exactly one value.",
            this
        )
    }

    fun values(i: Iteration, baseIRI: String?): List<Any?> {
        val list = mutableListOf<Any?>()

        // TODO: We assume that function maps, parameter maps, and input value maps only yield one value
        val functions = functionMap?.generateIRIs(i, baseIRI) ?: throw BurpException(
            RMLError.OutOfSpec,
            "Function map is required for function execution.",
            this
        )
        if (functions.size != 1) throwExactlyOneValueError("Function map")

        val functionUri = functions.first().asResource().uri

        // Bind parameters via a map
        val parameterMap = mutableMapOf<String, Any?>()

        for (input in inputs) {
            val parameters = input.parameterMap.generateIRIs(i, baseIRI)
            if (parameters.size != 1) throwExactlyOneValueError("Parameter map")
            val parameterUri = parameters.first().asResource().uri

            val inputs = input.inputValueMap.generateTerms(i, baseIRI)
            if (inputs.size != 1) throwExactlyOneValueError("Input value map")
            val firstInput = inputs.first()
            val inputValue = if (firstInput.isResource) firstInput else firstInput.asLiteral()

            parameterMap[parameterUri] = inputValue
        }

        for (returnValue in FunctionsRegistry.execute(functionUri, parameterMap)) {
            // if return map is null, then we return the default return value
            // Otherwise, look for the value identified by the return map
            if (returnMap == null) {
                list.add(returnValue.defaultValue)
            } else {
                val returnsMapIris = returnMap!!.generateIRIs(i, baseIRI)
                if (returnsMapIris.size != 1) throwExactlyOneValueError("Return map")

                val v = returnValue.get(returnsMapIris.first().asResource().uri)
                    ?: throw BurpException(
                        RMLError.ExecutionError,
                        "Return value ${returnsMapIris.first()} no known.",
                        this
                    )
                list.add(v)
            }
        }

        return list
    }

    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null
}