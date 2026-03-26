package burp.model.fnmlutil

import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER

interface RMLFunction {
    val name: String
    fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return>
}

/**
 * Get parameter value, the parameter must be in the map, but it can be null, or exit with an error
 */
fun RMLFunction.getRequiredParameter(parameters: Map<String, Any?>, parameterName: String, origin: Origin?): Any? {
    if (!parameters.containsKey(parameterName)) {
        throw BurpException(
            RmlError(
                "Required parameter '$parameterName' is missing for function $name.",
                origin,
                RER.FunctionExecutionError
            )
        )
    }
    return parameters[parameterName]
}

class RMLFunctionException(message: String, throwable: Throwable, function: RMLFunction) :
    RuntimeException(message, throwable)