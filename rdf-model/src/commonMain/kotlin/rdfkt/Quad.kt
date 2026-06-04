package rdfkt

import rdf.BlankNode
import rdf.NamedNode
import rdf.Quad
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

sealed interface Term : rdf.Term

sealed interface BlankNodeOrIRI : Term

sealed interface Graph : Term

@JvmInline
value class BlankTerm(override val value: String) : BlankNodeOrIRI, Graph, BlankNode {
    override val termType: String get() = "BlankNode"
    override fun toString() = "_:$value"

    companion object {
        fun from(i: Int) = BlankTerm("A$i")
    }
}

@JvmInline
value class NamedTerm(override val value: String) : BlankNodeOrIRI, Graph, NamedNode {
    override val termType: String get() = "NamedNode"
    override fun toString() = "<$value>"

    override val uri: String get() = value
}

data class Literal(
    override val value: String,
    val type: NamedTerm? = null,
    val lang: String? = null
) : Term, rdf.Literal {
    override val termType: String get() = "Literal"
    override val language: String get() = lang ?: ""
    override val datatype: NamedNode get() = type ?: XSD.string

    override fun toString(): String =
        when {
            lang != null -> "\"$value\"@$lang"
            type != null -> "\"$value\"^^${type.value}"
            else -> "\"$value\""
        }
}

data object DefaultGraph : Graph, rdf.DefaultGraph {
    override val termType: String get() = "DefaultGraph"
    override val value: String get() = ""
}

data class Quad(
    val s: BlankNodeOrIRI,
    val p: NamedTerm,
    val o: Term,
    val g: Graph = DefaultGraph
) : BlankNodeOrIRI, Quad {
    override val termType: String get() = "Quad"
    override val value: String get() = toString()

    override val subject: rdf.Term get() = s
    override val predicate: rdf.Term get() = p
    override val `object`: rdf.Term get() = o
    override val graph: rdf.Term get() = g

    override fun toString() = "$s $p $o $g"

    companion object {

        @JvmStatic
        fun String.asNamedTerm() = NamedTerm(this)

        @JvmStatic
        fun String.asLiteralTerm() = Literal(this, type = XSD.string)

        @JvmStatic
        fun Int.asLiteralTerm() = Literal(toString(), type = XSD.int)

        @JvmStatic
        fun Long.asLiteralTerm() = Literal(toString(), type = XSD.long)

        @JvmStatic
        fun Float.asLiteralTerm() = Literal(toString(), type = XSD.float)

        @JvmStatic
        fun Double.asLiteralTerm() = Literal(toString(), type = XSD.double)

        @JvmStatic
        fun Boolean.asLiteralTerm() = Literal(toString(), type = XSD.boolean)

        @JvmStatic
        fun Number.asLiteralTerm() = when (this) {
            is Int -> asLiteralTerm()
            is Long -> asLiteralTerm()
            is Float -> asLiteralTerm()
            else -> toDouble().asLiteralTerm()
        }

        @JvmStatic
        fun <T> T.asLiteralTerm() = when (this) {
            is Number -> asLiteralTerm()
            is String -> asLiteralTerm()
            is Boolean -> asLiteralTerm()
            else -> throw IllegalArgumentException("Unknown literal type `$this`")
        }

    }
}

fun rdf.Term.toRdfkt(): rdfkt.Term = when (this) {
    is rdfkt.Term -> this
    is rdf.NamedNode -> rdfkt.NamedTerm(this.value)
    is rdf.BlankNode -> rdfkt.BlankTerm(this.value)
    is rdf.Literal -> rdfkt.Literal(this.value, this.datatype.let { rdfkt.NamedTerm(it.value) }, this.language.ifEmpty { null })
    is rdf.Quad -> rdfkt.Quad(
        this.subject.toRdfkt() as rdfkt.BlankNodeOrIRI,
        this.predicate.toRdfkt() as rdfkt.NamedTerm,
        this.`object`.toRdfkt(),
        this.graph.toRdfkt() as rdfkt.Graph
    )
    is rdf.DefaultGraph -> rdfkt.DefaultGraph
    else -> throw IllegalArgumentException("Unknown term type: ${this::class.simpleName} value: $this")
}
