package burp.model.fnmlutil

import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import java.util.*


object FunctionsRegistry {
    var functions: Map<String, RMLFunction> = loadFunctions()

    fun execute(function: String, map: Map<String, Any?>, expressionOrigin: Origin?): List<Return> {
        val f = functions[function]
        if (f != null) return f.apply(map, expressionOrigin)
        throw BurpException(
            RmlError(
                "UnsupportedFunction: Function $function not yet supported.",
                expressionOrigin,
                RER.UnsupportedFunction
            )
        )
    }
}

private fun loadFunctions(): Map<String, RMLFunction> {
    val load = ServiceLoader.load(RMLFunction::class.java)
    val functions = mutableMapOf<String, RMLFunction>()
    load.forEach { f ->
        if (!functions.containsKey(f.name)) {
            functions[f.name] = f
        } else {
            //FIXME Should be a Warning log
            println("Function " + f.name + " already exists, not loading from service loader $f.")
        }
    }
    println("The following ${functions.size} functions were loaded:")
    functions.keys.forEach { println("- $it") }
    return functions
}