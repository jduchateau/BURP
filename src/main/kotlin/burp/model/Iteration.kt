package burp.model

abstract class Iteration(@JvmField var nulls: Set<Any?>) {
    abstract fun asString(): String?
}