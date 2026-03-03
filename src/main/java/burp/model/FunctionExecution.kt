package burp.model

import burp.model.fnmlutil.FunctionsRegistry
import burp.reporting.BurpException
import burp.reporting.Origin

class FunctionExecution : Expression() {
    var functionMap: FunctionMap? = null
    var inputs: MutableList<Input> = ArrayList<Input>()
    var returnMap: ReturnMap? = null

    @Throws(BurpException::class)
    fun values(iteration: Iteration?, baseIRI: String?, expressionOrigin: Origin?): MutableList<Any?> {
        val list: MutableList<Any?> = ArrayList<Any?>()

        // TODO: We assume that function maps, parameter maps, and input value maps only yield one value
        val functions = functionMap!!.generateIRIs(iteration, baseIRI)
        if (functions.size != 1) throw RuntimeException("Function map should generate exactly one value.")

        val function = functions[0]!!.asResource().uri

        // Bind parameters via a map
        val map = mutableMapOf<String, Any?>()

        for (input in inputs) {
            val parameters = input.parameterMap.generateIRIs(iteration, baseIRI)
            if (parameters.size != 1) throw RuntimeException("Parameter map should generate exactly one value.")

            val parameter = parameters[0]!!.asResource().uri

            val inputs = input.inputValueMap.generateTerms(iteration, baseIRI)
            if (inputs.size != 1) throw RuntimeException("Input value map should generate exactly one value.")

            val inputValue: Any? = if (inputs[0]!!.isResource) inputs[0] else inputs[0]!!.asLiteral()

            map[parameter] = inputValue
        }

        for (o in FunctionsRegistry.execute(function, map, expressionOrigin)) {
            // if return map is null, then we return the default return value
            // Otherwise, look for the value identified by the return map
            if (returnMap == null) {
                list.add(o.defaultValue)
            } else {
                val returns = returnMap!!.generateIRIs(iteration, baseIRI)
                if (returns.size != 1) throw RuntimeException("Input value map should generate exactly one value.")

                val v = o.get(returns[0]!!.asResource().uri, expressionOrigin)
                list.add(v)
            }
        }

        return list
    }
}