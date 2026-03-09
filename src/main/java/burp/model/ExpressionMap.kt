package burp.model

import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.util.isValidAndAbsoluteIRI
import burp.util.isValidAndAbsoluteURI
import burp.vocabularies.RER
import org.apache.jena.datatypes.BaseDatatype
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.util.URIref
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

abstract class ExpressionMap {
    var expression: Expression? = null
    var expressionOrigin: Origin? = null

    fun generateValues(i: Iteration, baseIRI: String, safe: Boolean): List<Any?> {
        return when (val expr = expression) {
            is RDFNodeConstant -> listOfNotNull(expr.constant)
            is Template -> expr.values(i, safe)
            is Reference -> expr.values(i)
            is FunctionExecution -> expr.values(i, baseIRI)
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
    fun generateUnsafeIRIs(i: Iteration, baseIRI: String): List<String> =
        generateValues(i, baseIRI, false).map {
            when (it) {
                is RDFNode -> it.asResource().uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteIRI(URIref.encode(string))) string
                    else if (isValidAndAbsoluteIRI(URIref.encode(baseIRI + string))) baseIRI + string
                    else throw BurpException(
                        RmlError(
                            "$baseIRI and $string do not constitute a valid UnsafeIRI",
                            expressionOrigin,
                            RER.InvalidIRI
                        )
                    )
                }
            }
        }

    // Generate absolute percent-encoded IRI
    fun generateIRIs(i: Iteration, baseIRI: String): List<String> =
        generateValues(i, baseIRI, true).map {
            when (it) {
                is RDFNode if it.isResource -> it.asResource().uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        is Literal -> it.lexicalForm
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteIRI(string)) string
                    else if (isValidAndAbsoluteIRI(baseIRI + string)) baseIRI + string
                    else throw BurpException(
                        RmlError(
                            "$baseIRI and $string do not constitute a valid IRI",
                            expressionOrigin,
                            RER.InvalidIRI
                        )
                    )
                }
            }
        }


    // Generate absolute non-percent-encoded URI
    fun generateUnsafeURIs(i: Iteration, baseIRI: String): List<String> =
        generateValues(i, baseIRI, false).map {
            when (it) {
                is RDFNode -> it.asResource().uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteURI(URIref.encode(string))) string
                    else if (isValidAndAbsoluteURI(URIref.encode(baseIRI + string))) baseIRI + string
                    else throw BurpException(
                        RmlError(
                            "$baseIRI and $string do not constitute a valid UnsafeURI",
                            expressionOrigin,
                            RER.InvalidURI
                        )
                    )
                }
            }

        }

    // Generate absolute percent-encoded URI
    fun generateURIs(i: Iteration, baseIRI: String): List<String> =
        generateValues(i, baseIRI, true).map {
            when (it) {
                is RDFNode -> it.asResource().uri
                else -> {
                    val string = when (it) {
                        is String -> it
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteURI(string)) string
                    else if (isValidAndAbsoluteURI(baseIRI + string)) baseIRI + string
                    else throw BurpException(
                        RmlError(
                            "$baseIRI and $string do not constitute a valid URI",
                            expressionOrigin,
                            RER.InvalidURI
                        )
                    )
                }
            }

        }

    protected fun generateBlankNodes(i: Iteration, baseIRI: String): List<RDFNode> {
        fun blankNodeFor(value: Any?): RDFNode =
            blankNodeMap.computeIfAbsent(value) { ResourceFactory.createResource() }

        return when (val expr = expression) {
            is RDFNodeConstant -> listOfNotNull(expr.constant?.asResource())
            is Template -> expr.values(i).map { blankNodeFor(it) }
            is Reference -> expr.values(i).map { blankNodeFor(it) }
            is FunctionExecution -> expr.values(i, baseIRI).map { blankNodeFor(it) }
            null -> listOf(ResourceFactory.createResource())
            else -> throw RuntimeException("Error generating blank node.")
        }
    }

    protected fun generateLiterals(
        i: Iteration,
        baseIRI: String,
        dm: DatatypeMap?,
        lm: LanguageMap?
    ): List<RDFNode> {
        val expr = expression
        val datatypes = dm?.generateIRIs(i, baseIRI)
        val languages = lm?.generateLanguageTags(i, baseIRI)

        fun literalFor(value: Any?): List<RDFNode> {
            return when {
                value == null -> listOf()
                languages != null -> languages.map { ResourceFactory.createLangLiteral(value.toString(), it) }
                datatypes != null -> datatypes.map {
                    ResourceFactory.createTypedLiteral(value.toString(), BaseDatatype(it))
                }

                else -> listOf(createTypedLiteral(value))
            }
        }

        return when (expr) {
            is RDFNodeConstant -> listOfNotNull(expr.constant)
            is Template -> expr.values(i).flatMap { literalFor(it) }
            is Reference -> expr.values(i).flatMap { literalFor(it) }
            is FunctionExecution -> expr.values(i, baseIRI).flatMap { literalFor(it) }
            else -> throw RuntimeException("Error generating literal or value.")
        }
    }

    private fun createTypedLiteral(o: Any?): Literal {
        when (o) {
            is Int, is Long -> return ResourceFactory.createTypedLiteral(o.toString(), XSDDatatype.XSDinteger)
            is Float -> {
                val s: String = doubleCanonicalMap(o.toString().toDouble())
                return ResourceFactory.createTypedLiteral(s, XSDDatatype.XSDdouble)
            }

            is Double -> {
                val s: String = doubleCanonicalMap(o)
                return ResourceFactory.createTypedLiteral(s, XSDDatatype.XSDdouble)
            }

            is Date -> {
                return ResourceFactory.createTypedLiteral(o.toString(), XSDDatatype.XSDdate)
            }

            is Timestamp -> {
                var s = o.toString().replace(" ", "T")


                // Ensure canonical xsd:dateTime by removing the ".0" when no fraction
                if (o.getNanos() == 0) s = s.replace(".0", "")

                return ResourceFactory.createTypedLiteral(s, XSDDatatype.XSDdateTime)
            }

            is Literal -> {
                return o
            }

            else -> return ResourceFactory.createTypedLiteral(o)
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
        private val blankNodeMap = mutableMapOf<Any?, RDFNode>()
    }
}