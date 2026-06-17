package rdfkt

import rdf.BlankNode
import rdf.NamedNode

// Dummy declarations for JS compilation compatibility of KSP generated mappers
class JenaNamedNode(val node: Any) : NamedNode {
    override val value: String get() = ""
    override fun equals(other: Any?): Boolean = other is NamedNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

class JenaBlankNode(val node: Any) : BlankNode {
    override val value: String get() = ""
    override fun equals(other: Any?): Boolean = other is BlankNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
}
