package burp.model

sealed interface Term

sealed interface BlankNodeOrIRI : Term

data class BlankNodeTerm(val id: String) : BlankNodeOrIRI {
    override fun toString() = "_:$id"
}

data class IRITerm(val uri: String) : BlankNodeOrIRI {
    override fun toString() = "<$uri>"
}

data class LiteralTerm(
    val value: String, val datatype: IRITerm? = null, val language: String? = null
) : Term {
    override fun toString(): String = when {
        language != null -> "\"$value\"@$language"
        datatype != null -> "\"$value\"^^${datatype.uri}"
        else -> "\"$value\""
    }
}

typealias GraphId = IRITerm?

// -----------------------------------------------------
// Container and Collection Structures for RML-CC
// -----------------------------------------------------

sealed class CollectionOrContainerTerm(open val idGenerated: Boolean) : BlankNodeOrIRI {
    abstract val elements: MutableList<Term>
    abstract var id: BlankNodeOrIRI
}

data class RdfListTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

data class RdfBagTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

data class RdfSeqTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

data class RdfAltTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

// -----------------------------------------------------
// Statements and partial statements
// -----------------------------------------------------

data class RdfPredicateObject(
    val predicate: IRITerm, val `object`: Term, val graph: GraphId = null
)

sealed interface RdfStatementLike

data class RdfStatement(
    var subject: BlankNodeOrIRI, var predicate: IRITerm, var `object`: Term, var graph: GraphId = null
) : RdfStatementLike

data class RdfStatementSubjectGraph(
    var subject: BlankNodeOrIRI, var graph: GraphId = null
) : RdfStatementLike

// -----------------------------------------------------
// Equality check for values (handling native types)
// -----------------------------------------------------

/**
 * Compares two values for semantic equality, particularly useful for Join Conditions.
 * It natively compares objects of the same class (for performance) and otherwise
 * falls back to converting native types to RDF LiteralTerms to ensure semantic
 * equality (e.g., matching a raw String with a LiteralTerm of xsd:string).
 */
fun valuesMatch(a: Any?, b: Any?): Boolean {
    if (a == b) return true

    if (a != null && b != null) {
        // If not Term and same type, native equal is enough to tell it is different.
        if (a::class == b::class && a !is Term) {
            return false
        }
    }

    val termA = toTerm(a)
    val termB = toTerm(b)

    if (termA is LiteralTerm && termB is LiteralTerm) {
        if (termA.value != termB.value) return false
        if (termA.language != termB.language) return false

        val dtA = termA.datatype?.uri ?: "http://www.w3.org/2001/XMLSchema#string"
        val dtB = termB.datatype?.uri ?: "http://www.w3.org/2001/XMLSchema#string"
        if (dtA != dtB) return false
    }

    return termA == termB
}

internal val XSDinteger = IRITerm("http://www.w3.org/2001/XMLSchema#integer")
internal val XSDdouble = IRITerm("http://www.w3.org/2001/XMLSchema#double")
internal val XSDdate = IRITerm("http://www.w3.org/2001/XMLSchema#date")
internal val XSDdateTime = IRITerm("http://www.w3.org/2001/XMLSchema#dateTime")
internal val XSDstring = IRITerm("http://www.w3.org/2001/XMLSchema#string")

fun toTerm(o: Any?): Term? {
    if (o is Term) return o
    if (o == null) return null

    when (o) {
        is Int, is Long -> return LiteralTerm(o.toString(), datatype = XSDinteger)
        is Float -> return LiteralTerm(doubleCanonicalMap(o.toString().toDouble()), datatype = XSDdouble)
        is Double -> return LiteralTerm(doubleCanonicalMap(o), datatype = XSDdouble)
        is java.sql.Date -> return LiteralTerm(o.toString(), datatype = XSDdate)
        is java.sql.Timestamp -> {
            var s = o.toString().replace(" ", "T")
            if (o.nanos == 0) s = s.replace(".0", "")
            return LiteralTerm(s, datatype = XSDdateTime)
        }

        else -> return LiteralTerm(o.toString(), datatype = XSDstring)
    }
}

internal fun doubleCanonicalMap(d: Double): String {
    val f = java.math.BigDecimal.valueOf(d)
    val p = f.precision()
    val x = "0.0" + "#".repeat(kotlin.math.max(0, p - 2)) + "E0"
    val numberFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    val formatter = numberFormat as java.text.DecimalFormat
    formatter.applyPattern(x)
    return formatter.format(d)
}