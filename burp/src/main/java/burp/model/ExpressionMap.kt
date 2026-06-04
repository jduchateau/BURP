package burp.model

import burp.model.TemplateReferenceSafety.*
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.util.isValidAndAbsoluteIRI
import burp.util.isValidAndAbsoluteURI
import burp.vocabularies.RER
import burp.vocabularies.Rml
import org.apache.jena.util.URIref
import rdfobjectloader.PointRange
import rdfobjectloader.RDFPointer
import rdfobjectloader.annotations.OriginOfProperty
import rdfobjectloader.annotations.RdfMappedFrom
import rdfobjectloader.annotations.RdfProperty

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
abstract class ExpressionMap : LogicalTargetScope, PlanNode {

    @RdfMappedFrom([Template::class, RawReference::class, RDFNodeConstant::class, FunctionExecution::class])
    var expression: Expression? = null

    @OriginOfProperty("expression")
    var expressionOrigin: RDFPointer? = null


    internal fun origin() = Origin(this, listOfNotNull(expressionOrigin))

    @RdfProperty(Rml.logicalTarget)
    override val logicalTargets: MutableSet<LogicalTarget> = mutableSetOf()

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (expression != null) yield(expression!!)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    private val baseIRI = lazy { ancestor<BaseIRIScope>()!!.getBaseIri() }

    fun generateValues(i: Iteration, safe: TemplateReferenceSafety): List<Any?> {
        return when (val expr = expression) {
            is RDFNodeConstant -> listOfNotNull(toTerm(expr.constant))
            is Template -> expr.values(i, safe)
            is Reference -> expr.values(i)
            is FunctionExecution -> expr.values(i)
            else -> throw BurpException(
                RmlError(
                    "Error generating values, expression is not supported.",
                    origin(),
                    RER.UnsupportedMapping
                )
            )
        }
    }

    // Generate absolute non-percent-encoded IRI
    //TODO: Convert the character to a sequence of one or more octets using UTF-8 in [RFC3629] for all (Unsafe-)URI/IRI
    fun generateUnsafeIRIs(i: Iteration): List<IRITerm> {
        val targets = getEffectiveTargets()
        return generateValues(i, Unsafe).map {
            when (it) {
                is IRITerm -> IRITerm(it.uri, targets)
                else -> {
                    val string = when (it) {
                        is String -> it
                        is LiteralTerm -> it.value
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteIRI(URIref.encode(string))) IRITerm(string, targets)
                    else if (isValidAndAbsoluteIRI(URIref.encode(baseIRI.value + string))) IRITerm(
                        baseIRI.value + string,
                        targets
                    )
                    else throw BurpException(
                        RmlError(
                            "${baseIRI.value} and $string do not constitute a valid UnsafeIRI",
                            origin(),
                            RER.InvalidIRI
                        )
                    )
                }
            }
        }
    }


    // Generate absolute percent-encoded IRI
    fun generateIRIs(i: Iteration): List<IRITerm> {
        val targets = getEffectiveTargets()
        return generateValues(i, SafeIRI).map {
            when (it) {
                is IRITerm -> IRITerm(it.uri, targets)
                else -> {
                    val string = when (it) {
                        is String -> it
                        is LiteralTerm -> it.value
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteIRI(string)) IRITerm(string, targets)
                    else if (isValidAndAbsoluteIRI(baseIRI.value + string)) IRITerm(baseIRI.value + string, targets)
                    else throw BurpException(
                        RmlError(
                            "${baseIRI.value} and $string do not constitute a valid IRI",
                            origin(),
                            RER.InvalidIRI
                        )
                    )
                }
            }
        }
    }

    // Generate absolute percent-encoded URI
    fun generateURIs(i: Iteration): List<IRITerm> {
        val targets = getEffectiveTargets()
        return generateValues(i, SafeURI).map {
            when (it) {
                is IRITerm -> IRITerm(it.uri, targets)
                else -> {
                    val string = when (it) {
                        is String -> it
                        else -> it.toString()
                    }
                    if (isValidAndAbsoluteURI(string)) IRITerm(string, targets)
                    else if (isValidAndAbsoluteURI(baseIRI.value + string)) IRITerm(baseIRI.value + string, targets)
                    else throw BurpException(
                        RmlError(
                            "${baseIRI.value} and $string do not constitute a valid URI",
                            origin(),
                            RER.InvalidURI
                        )
                    )
                }
            }

        }
    }

    protected fun generateBlankNodes(i: Iteration): List<BlankNodeTerm> {
        val targets = getEffectiveTargets()
        fun blankNodeFor(value: Any?): BlankNodeTerm {
            val id = blankNodeMap.computeIfAbsent(value) { "bnode-${blankNodeIdCounter++}" }
            return BlankNodeTerm(id, targets)
        }

        return when (val expr = expression) {
            is RDFNodeConstant -> {
                val term = toTerm(expr.constant)
                if (term is BlankNodeTerm) {
                    listOf(BlankNodeTerm(term.id, targets))
                } else {
                    val name = (this as? TermMap)?.getName() ?: "constant"
                    throw BurpException(
                        burp.reporting.IncorrectTermType(
                            name,
                            rdfkt.NamedTerm(burp.vocabularies.Rml.BlankNode),
                            setOf(if (term is LiteralTerm) rdfkt.NamedTerm(burp.vocabularies.Rml.Literal) else rdfkt.NamedTerm(burp.vocabularies.Rml.IRI)),
                            this
                        )
                    )
                }
            }

            is Template -> expr.values(i, Unsafe).map { blankNodeFor(it) }
            is Reference -> expr.values(i).map { blankNodeFor(it) }
            is FunctionExecution -> expr.values(i).map { blankNodeFor(it) }
            null -> listOf(BlankNodeTerm("bnode-${blankNodeIdCounter++}", targets))
            else -> throw RuntimeException("Error generating blank node.")
        }
    }

    private fun intersectTargets(t1: Set<LogicalTarget>, t2: Set<LogicalTarget>): Set<LogicalTarget> {
        if (t1.isEmpty()) return t2
        if (t2.isEmpty()) return t1
        return t1.intersect(t2)
    }

    protected fun generateLiterals(i: Iteration, dm: DatatypeMap?, lm: LanguageMap?): List<LiteralTerm> {
        val expr = expression
        val datatypes = dm?.generateIRIs(i)
        val languages = lm?.generateLanguageTags(i)
        val baseTargets = getEffectiveTargets()

        fun literalFor(value: Any?): List<LiteralTerm> {
            return when {
                value == null -> listOf()
                languages != null -> languages.map { langTag ->
                    LiteralTerm(
                        value.toString(),
                        language = langTag.tag,
                        targets = intersectTargets(baseTargets, langTag.targets)
                    )
                }

                datatypes != null -> datatypes.map { dt ->
                    LiteralTerm(value.toString(), datatype = dt, targets = intersectTargets(baseTargets, dt.targets))
                }

                else -> listOf(
                    (toTerm(value) as? LiteralTerm)?.copy(targets = baseTargets)
                        ?: LiteralTerm(value.toString(), targets = baseTargets)
                )
            }
        }

        return when (expr) {
            is RDFNodeConstant -> {
                val term = toTerm(expr.constant)
                if (term is LiteralTerm) {
                    listOf(term.copy(targets = baseTargets))
                } else {
                    val name = (this as? TermMap)?.getName() ?: "constant"
                    throw BurpException(
                        burp.reporting.IncorrectTermType(
                            name,
                            rdfkt.NamedTerm(burp.vocabularies.Rml.Literal),
                            setOf(if (term is BlankNodeTerm) rdfkt.NamedTerm(burp.vocabularies.Rml.BlankNode) else rdfkt.NamedTerm(burp.vocabularies.Rml.IRI)),
                            this
                        )
                    )
                }
            }

            is Template -> expr.values(i, Unsafe).flatMap { literalFor(it) }
            is Reference -> expr.values(i).flatMap { literalFor(it) }
            is FunctionExecution -> expr.values(i).flatMap { literalFor(it) }
            else -> throw RuntimeException("Error generating literal or value.")
        }
    }

    override fun nodeRanges(): List<PointRange> {
        val pointers = listOfNotNull(expressionOrigin)
        return turtleprov.retrieveTurtleLocation(pointers)
    }

    companion object {
        private var blankNodeIdCounter = 0L
        private val blankNodeMap = mutableMapOf<Any?, String>()
    }
}