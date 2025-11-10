package rdf

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic


sealed interface Term
sealed interface BlankNodeOrIRI : Term
sealed interface Graph

@JvmInline
value class BlankTerm(val value: String) : BlankNodeOrIRI, Graph {
    override fun toString() = "_:$value"

    companion object {
        fun from(i: Int) = BlankTerm("A$i")
    }
}

@JvmInline
value class NamedTerm(val value: String) : BlankNodeOrIRI, Graph {
    override fun toString() = "<$value>"

    val uri: String get() = value
}




data class Literal(
    val value: String,
    val type: NamedTerm? = null,
    val lang: String? = null
) : Term {
    override fun toString(): String =
        when {
            lang != null -> "\"$value\"@$lang"
            type != null -> "\"$value\"^^${type.value}"
            else -> "\"$value\""
        }
}

data object DefaultGraph : Graph

//data class PartialQuad(
//    val s: BlankNodeOrIRI?,
//    val p: NamedTerm?,
//    val o: Term?,
//    val g: Graph?
//) : BlankNodeOrIRI {
//    fun toQuadOrNull(): Quad? {
//        if (s != null && p != null && o != null && g != null) {
//            return Quad(s, p, o, g)
//        }
//        return null
//    }
//
//    fun toTripleOrNull(): Quad? {
//        if (s != null && p != null && o != null) {
//            return Quad(s, p, o, g ?: DefaultGraph)
//        }
//        return null
//    }
//}


data class Quad(
    val s: BlankNodeOrIRI,
    val p: NamedTerm,
    val o: Term,
    val g: Graph = DefaultGraph
) : BlankNodeOrIRI {

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


