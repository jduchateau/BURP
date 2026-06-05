package burp.model.fnmlutil

import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import burp.vocabularies.RML


class Return(defaultValue: Any?, vararg pairs: Pair<String, Any?>) {
    private val returns = mutableMapOf<String, Any?>()

    @JvmField
    var defaultValue: Any? = null

    init {
        this.defaultValue = defaultValue
        pairs.forEach { returns[it.first] = it.second }
    }

    fun get(key: String, origin: Origin?) =
        if (returns.contains(key)) returns[key]
        else throw BurpException(
            RmlError(
                "Function execution: No return value for $key choose one of ${returns.keys}.", origin, RER.FunctionExecutionError, // FIXME: Add FunctionNoReturnValueError
                context = mapOf( // TODO define keys for context in RER or find better keys in RML.
                    RML.return_ to key,
                    RML.returnMap to returns.keys.toString()
                )
            )
        )



    fun put(key: String, value: Any?) = returns.put(key, value)

}
