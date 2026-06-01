package rdfobjectloader.model

sealed interface Term {
    val termType: String
    val value: String
    override fun equals(other: Any?): Boolean
}

interface NamedNode : Term {
    override val termType: String get() = "NamedNode"
}

interface BlankNode : Term {
    override val termType: String get() = "BlankNode"
}

interface Literal : Term {
    override val termType: String get() = "Literal"
    val language: String
    val datatype: NamedNode
}

interface Variable : Term {
    override val termType: String get() = "Variable"
}

interface DefaultGraph : Term {
    override val termType: String get() = "DefaultGraph"
    override val value: String get() = ""
}
