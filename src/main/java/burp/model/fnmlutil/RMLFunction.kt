package burp.model.fnmlutil

import burp.reporting.Origin

interface RMLFunction {
    val name: String
    fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return>
}

class RMLFunctionException(message: String, throwable: Throwable, function: RMLFunction) :
    RuntimeException(message, throwable)