package burp.model

import burp.model.TemplateReferenceSafety.*
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.util.isValidAndAbsoluteIRI
import burp.util.isValidAndAbsoluteURI
import burp.vocabularies.RER
import org.apache.jena.util.URIref
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

/**
 * Natural RDF Mappings for Logical Sources:
 * When values are extracted from logical sources, they map to natural RDF types.
 *
 * JSON:
 * - number without fraction -> xsd:integer
 * - number with fraction -> xsd:double
 * - boolean -> xsd:boolean
 * - string -> xsd:string
 *
 * CSV:
 * - No native types. Uses xsd:string by default, unless csvw:datatype is specified in a CSVW Table.
 *
 * XML:
 * - Natural mapping directly uses XSD data types from XML. No transformation needed.
 *
 * SQL:
 * - BINARY/BLOB -> xsd:hexBinary
 * - NUMERIC/DECIMAL -> xsd:decimal
 * - SMALLINT/INTEGER/BIGINT -> xsd:integer
 * - FLOAT/REAL/DOUBLE PRECISION -> xsd:double
 * - BOOLEAN -> xsd:boolean
 * - DATE -> xsd:date
 * - TIME -> xsd:time
 * - TIMESTAMP -> xsd:dateTime (spaces replaced with T)
 *
 * RML-FNML:
 * - Values can be returned as explicit Terms or native datatypes.
 */
abstract class ExpressionMap : PlanNode {
    var expression: Expression? = null
    var expressionOrigin: Origin? = null

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (expression != null) yield(expression!!)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    private val baseIRI = lazy { ancestor<BaseIRIScope>()!!.getBaseIri() }

    fun generateValues(i: Iteration, safe: TemplateReferenceSafety): List<Any?> {
        return when (val expr = expression) {
            is RDFNodeConstant -> listOfNotNull(expr.constant)
            is Template -> expr.values(i, safe)
            is Reference -> expr.values(i)
            is FunctionExecution -> expr.values(i)
            else -> throw BurpException(
                RmlError(
                    "Error generating values, expression is not supported.",
                    expressionOrigin,
                    RER.UnsupportedMapping
                )
            )
        }
    }

    // Generate absolute non-percent-encoded IRI
    //TODO: Convert the character to a sequence of one or more octets using UTF-8 in [RFC3629] for all (Unsafe-)URI/IRI
    fun generateUnsafeIRIs(i: Iteration): List<String> =
        generateValues(i, Unsafe).map {
            when (it) {
                is IRITerm -> it.uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        is LiteralTerm -> it.value
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteIRI(URIref.encode(string))) string
                    else if (isValidAndAbsoluteIRI(URIref.encode(baseIRI.value + string))) baseIRI.value + string
                    else throw BurpException(
                        RmlError(
                            "${baseIRI.value} and $string do not constitute a valid UnsafeIRI",
                            expressionOrigin,
                            RER.InvalidIRI
                        )
                    )
                }
            }
        }


    // Generate absolute percent-encoded IRI
    fun generateIRIs(i: Iteration): List<String> {
        return generateValues(i, SafeIRI).map {
            when (it) {
                is IRITerm -> it.uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        is LiteralTerm -> it.value
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteIRI(string)) string
                    else if (isValidAndAbsoluteIRI(baseIRI.value + string)) baseIRI.value + string
                    else throw BurpException(
                        RmlError(
                            "${baseIRI.value} and $string do not constitute a valid IRI",
                            expressionOrigin,
                            RER.InvalidIRI
                        )
                    )
                }
            }
        }
    }

    // Generate absolute percent-encoded URI
    fun generateURIs(i: Iteration): List<String> =
        generateValues(i, SafeURI).map {
            when (it) {
                is IRITerm -> it.uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteURI(string)) string
                    else if (isValidAndAbsoluteURI(baseIRI.value + string)) baseIRI.value + string
                    else throw BurpException(
                        RmlError(
                            "${baseIRI.value} and $string do not constitute a valid URI",
                            expressionOrigin,
                            RER.InvalidURI
                        )
                    )
                }
            }

        }

    protected fun generateBlankNodes(i: Iteration): List<BlankNodeTerm> {
        fun blankNodeFor(value: Any?): BlankNodeTerm =
            blankNodeMap.computeIfAbsent(value) { BlankNodeTerm("bnode-${blankNodeIdCounter++}") }

        return when (val expr = expression) {
            is RDFNodeConstant -> listOfNotNull(expr.constant as? BlankNodeTerm)
            is Template -> expr.values(i, Unsafe).map { blankNodeFor(it) }
            is Reference -> expr.values(i).map { blankNodeFor(it) }
            is FunctionExecution -> expr.values(i).map { blankNodeFor(it) }
            null -> listOf(BlankNodeTerm("bnode-${blankNodeIdCounter++}"))
            else -> throw RuntimeException("Error generating blank node.")
        }
    }

    protected fun generateLiterals(i: Iteration, dm: DatatypeMap?, lm: LanguageMap?): List<LiteralTerm> {
        val expr = expression
        val datatypes = dm?.generateIRIs(i)
        val languages = lm?.generateLanguageTags(i)

        fun literalFor(value: Any?): List<LiteralTerm> {
            return when {
                value == null -> listOf()
                languages != null -> languages.map { LiteralTerm(value.toString(), language = it) }
                datatypes != null -> datatypes.map {
                    LiteralTerm(value.toString(), datatype = IRITerm(it))
                }

                else -> listOf(createTypedLiteral(value))
            }
        }

        return when (expr) {
            is RDFNodeConstant -> listOfNotNull(expr.constant as? LiteralTerm)
            is Template -> expr.values(i, Unsafe).flatMap { literalFor(it) }
            is Reference -> expr.values(i).flatMap { literalFor(it) }
            is FunctionExecution -> expr.values(i).flatMap { literalFor(it) }
            else -> throw RuntimeException("Error generating literal or value.")
        }
    }

    private fun createTypedLiteral(o: Any?): LiteralTerm {
        val XSDinteger = IRITerm("http://www.w3.org/2001/XMLSchema#integer")
        val XSDdouble = IRITerm("http://www.w3.org/2001/XMLSchema#double")
        val XSDdate = IRITerm("http://www.w3.org/2001/XMLSchema#date")
        val XSDdateTime = IRITerm("http://www.w3.org/2001/XMLSchema#dateTime")
        val XSDstring = IRITerm("http://www.w3.org/2001/XMLSchema#string") // Default

        when (o) {
            is Int, is Long -> return LiteralTerm(o.toString(), datatype = XSDinteger)
            is Float -> {
                val s: String = doubleCanonicalMap(o.toString().toDouble())
                return LiteralTerm(s, datatype = XSDdouble)
            }

            is Double -> {
                val s: String = doubleCanonicalMap(o)
                return LiteralTerm(s, datatype = XSDdouble)
            }

            is Date -> {
                return LiteralTerm(o.toString(), datatype = XSDdate)
            }

            is Timestamp -> {
                var s = o.toString().replace(" ", "T")
                if (o.nanos == 0) s = s.replace(".0", "")
                return LiteralTerm(s, datatype = XSDdateTime)
            }

            is LiteralTerm -> {
                return o
            }

            else -> return LiteralTerm(o.toString())
        }
    }

    private fun doubleCanonicalMap(d: Double): String {
        val f = BigDecimal.valueOf(d)
        // The number of digits in the unscaled value
        val p = f.precision()
        // We start from two digits
        // Add the remaining digits to the pattern
        val x = "0.0" + "#".repeat(max(0, p - 2)) +  // Let's not forget the e-notation
                "E0"

        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        val formatter = numberFormat as DecimalFormat
        formatter.applyPattern(x)

        return formatter.format(d)
    }

    companion object {
        private var blankNodeIdCounter = 0L
        private val blankNodeMap = mutableMapOf<Any?, BlankNodeTerm>()
    }
}