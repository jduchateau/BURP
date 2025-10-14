package burp.model

import burp.util.Util
import org.apache.jena.datatypes.BaseDatatype
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

abstract class ExpressionMap(@JvmField val expression: Expression?) : IPlanNode {

    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null

    fun generateIRIs(i: Iteration, baseIRI: String?): List<RDFNode> {
        val set: MutableList<RDFNode?> = ArrayList<RDFNode?>()

        if (expression is RDFNodeConstant) {
            // It is assumed to be an IRI, otherwise the shapes
            // Would have caught the error.
            set.add(expression.constant.asResource())
            return set.filterNotNull()
        }

        if (expression is Template) {
            for (v in expression.values(i, true)) {
                if (Util.isAbsoluteAndValidIRI(v)) set.add(ResourceFactory.createResource(v))
                else if (Util.isAbsoluteAndValidIRI(baseIRI + v)) set.add(ResourceFactory.createResource(baseIRI + v))
                else throw RuntimeException("$baseIRI and $v do not constitute a valid IRI")
            }
            return set.filterNotNull()
        }

        if (expression is Reference) {
            for (v in expression.values(i)) {
                val s: String = v.toString()

                if (Util.isAbsoluteAndValidIRI(s)) set.add(ResourceFactory.createResource(s))
                else if (Util.isAbsoluteAndValidIRI(baseIRI + s)) set.add(ResourceFactory.createResource(baseIRI + s))
                else throw RuntimeException("$baseIRI and $s do not constitute a valid IRI")
            }
            return set.filterNotNull()
        }

        if (expression is FunctionExecution) {
            for (v in expression.values(i, baseIRI)) {
                val s: String = v.toString()

                if (Util.isAbsoluteAndValidIRI(s)) set.add(ResourceFactory.createResource(s))
                else if (Util.isAbsoluteAndValidIRI(baseIRI + s)) set.add(ResourceFactory.createResource(baseIRI + s))
                else throw RuntimeException("$baseIRI and $s do not constitute a valid IRI")
            }
            return set.filterNotNull()
        }

        throw RuntimeException("Error generating IRI.")
    }

    protected fun generateBlankNodes(i: Iteration, baseIRI: String?): List<RDFNode?> =
        when (expression) {
            is RDFNodeConstant ->
                // It is assumed to be a BN, otherwise the shapes
                // Would have caught the error.
                listOf(map.computeIfAbsent(expression.constant.asResource()) { ResourceFactory.createResource() })

            is Template -> expression.values(i)
                .map { map.computeIfAbsent(it) { ResourceFactory.createResource() } }
                .toList()

            is Reference -> expression.values(i)
                .map { map.computeIfAbsent(it) { ResourceFactory.createResource() } }
                .toList()

            null ->
                // IF NO REFERENCE, TEMPLATE, OR CONSTANT,
                // THEN WE GENERATE BLANK NODES (BASED ON THE ITERATION)
                listOf(ResourceFactory.createResource())

            is FunctionExecution -> expression.values(i, baseIRI)
                .map { map.computeIfAbsent(it) { ResourceFactory.createResource() } }
                .toList()

            else -> throw RuntimeException("Error generating blank node.")
        }


    protected fun generateLiterals(
        i: Iteration,
        baseIRI: String?,
        dm: DatatypeMap?,
        lm: LanguageMap?
    ): List<RDFNode?> {

        if (expression is RDFNodeConstant) {
            // It is assumed to be a literal, otherwise the shapes
            // Would have caught the error.
            return listOf(expression.constant)
        }

        val datatypes = dm?.generateIRIs(i, baseIRI)
        val languages = lm?.generateStrings(i, baseIRI)

        return when (expression) {
            is Template -> expression.values(i).flatMap { createTypedLiterals(it, languages, datatypes) }.toList()
            is Reference -> expression.values(i).flatMap { createTypedLiterals(it, languages, datatypes) }.toList()
            is FunctionExecution -> expression.values(i, baseIRI)
                .flatMap { createTypedLiterals(it!!, languages, datatypes) }
                .toList()

            else ->
                throw RuntimeException("Error generating literal or value.")

        }
    }

    private fun createTypedLiterals(
        value: Any,
        languages: Collection<String>?,
        datatypes: Collection<RDFNode>?
    ): List<Literal?> = when {
        languages != null -> languages.map { lang -> ResourceFactory.createLangLiteral(value.toString(), lang) }
            .toList()

        datatypes != null -> datatypes.map { datatype ->
            val datatypeUri = datatype.asResource().uri
            ResourceFactory.createTypedLiteral(value.toString(), BaseDatatype(datatypeUri))
        }.toList()

        else -> listOf(createTypedLiteral(value))
    }

    private fun createTypedLiteral(o: Any?): Literal? {
        return when (o) {
            is Int, is Long -> ResourceFactory.createTypedLiteral(o.toString(), XSDDatatype.XSDinteger)
            is Float -> {
                val s: String = doubleCanonicalMap(o.toString().toDouble())
                ResourceFactory.createTypedLiteral(s, XSDDatatype.XSDdouble)
            }

            is Double -> {
                val s: String = doubleCanonicalMap(o)
                ResourceFactory.createTypedLiteral(s, XSDDatatype.XSDdouble)
            }

            is Date -> ResourceFactory.createTypedLiteral(o.toString(), XSDDatatype.XSDdate)


            is Timestamp -> {
                var s: String = o.toString().replace(" ", "T")

                // Ensure canonical xsd:dateTime by removing the ".0" when no fraction
                if (o.nanos == 0) s = s.replace(".0", "")

                ResourceFactory.createTypedLiteral(s, XSDDatatype.XSDdateTime)
            }

            is Literal -> o
            else -> ResourceFactory.createTypedLiteral(o)
        }
    }

    companion object {
        private val map: MutableMap<Any?, RDFNode?> = HashMap<Any?, RDFNode?>()

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
    }
}