package burp.model

import burp.reporting.Origin

abstract class Iteration(@JvmField var nulls: Set<Any?>) {
    abstract fun getValuesFor(reference: String?, origin: Origin): List<Any?>

    abstract fun getStringsFor(reference: String?, origin: Origin): List<String>

    abstract fun asString(): String?
}