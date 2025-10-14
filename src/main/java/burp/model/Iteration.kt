package burp.model

abstract class Iteration(nulls: Set<Any?>?) {
    @JvmField
    protected val nulls: Set<Any?> = nulls ?: setOf()
    abstract fun getValuesFor(reference: String?): List<Any?>
    abstract fun getStringsFor(reference: String?): List<String?>
}
