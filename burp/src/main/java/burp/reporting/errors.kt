package burp.reporting

import burp.model.PlanNode
import burp.vocabularies.BURP
import burp.vocabularies.RER
import burp.vocabularies.RML
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntProperty
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import rdf.Quad
import rdfkt.JenaQuad
import rdfobjectloader.*
import java.nio.file.Path

data class Origin(
    /// The plan node in which the issue occurred, if exists (during parsing we may not plan nodes).
    val planNode: PlanNode? = null,
    // Statements at the source of the issue, if exists
    // (as soon as we have an RDF graph, we should have statements to pinpoints)
    val sourceStatements: List<RDFPointer>? = null,
) {

    constructor(planNode: PlanNode, sourceStatement: RDFPointer) : this(
        planNode,
        sourceStatements = listOf(sourceStatement)
    )

    constructor(stmt: Quad, vararg stmtParts: StatementPart) : this(
        sourceStatements = listOf(
            StatementParts(
                stmt,
                subject = StatementPart.Subject in stmtParts,
                predicate = StatementPart.Predicate in stmtParts,
                `object` = StatementPart.Object in stmtParts
            )
        )
    )

    constructor(stmt: Statement, vararg stmtParts: StatementPart) : this(JenaQuad(stmt), *stmtParts)

    constructor(planNode: PlanNode, stmt: Quad, vararg stmtParts: StatementPart) : this(
        planNode = planNode, sourceStatements = listOf(
            StatementParts(
                stmt,
                subject = StatementPart.Subject in stmtParts,
                predicate = StatementPart.Predicate in stmtParts,
                `object` = StatementPart.Object in stmtParts
            )
        )
    )
}

@Deprecated("Prefer passing teh first parameter as a Quad directly")
fun StatementParts.Companion.fromPredicateObject(stmt: Statement): StatementParts =
    StatementParts.fromPredicateObject(JenaQuad(stmt))

@Deprecated("Prefer passing teh first parameter as a Quad directly")
fun StatementParts.Companion.fromObject(stmt: Statement): StatementParts =
    StatementParts.fromObject(JenaQuad(stmt))

@Deprecated("Prefer passing teh first parameter as a Quad directly")
fun StatementParts.Companion.from(stmt: Statement, vararg parts: StatementPart): StatementParts =
    StatementParts.from(JenaQuad(stmt), *parts)

fun fileLocationString(file: Path, location: PointRange?): String {
    val locationStr = location?.let {
        val start = "${it.start.displayLine}:${it.start.displayColumn}"
        val end = it.end?.let { end -> "${end.displayLine}:${end.displayColumn}" }
        if (end != null) "$start-$end" else start
    } ?: ""

    return if (locationStr.isNotEmpty()) "$file:$locationStr" else file.toString()
}

class BurpException(val error: RmlError) : RuntimeException(error.message)

class RmlError(
    val message: String,
    val origin: Origin?,
    val errorType: OntClass,
    val exception: Exception? = null,
    val context: Map<Property, Any?> = emptyMap()
) {
    init {
        //assert(errorType.hasSuperClass(RER.Error, false)) { "$errorType is not a subClass of ${RER.Error}. It is ${errorType.listSuperClasses(false).toList()}" }
    }
}


class RmlErrorBuilder(val errorType: OntClass) {
    var message: String = ""
    var origin: Origin? = null
    var exception: Exception? = null
    private val _context = mutableMapOf<Property, Any>()

    fun reference(ref: String) {
        _context[RER.reference] = ref
    }

    fun referenceFormulation(refForm: Resource) {
        _context[RER.referenceFormulation] = refForm
    }

    fun allowedTermTypes(termTypes: List<Resource>) {
        _context[RER.allowedTermTypes] = termTypes
    }

    // Generic way to add any property from the RER vocabulary
    fun withContext(property: OntProperty, value: Any) {
        _context[property] = value
    }

    fun build() = RmlError(message, origin, errorType, exception, _context)
}


fun rmlError(errorType: OntClass, init: RmlErrorBuilder.() -> Unit): RmlError {
    val builder = RmlErrorBuilder(errorType)
    builder.init()
    return builder.build()
}


@Suppress("FunctionName")
fun SourceAccessError(message: String, info: Origin?, ex: Exception?) =
    RmlError(message, info, RER.SourceAccessError, ex)

@Suppress("FunctionName")
fun InvalidRDF(message: String, info: Origin?, type: OntClass) = RmlError(message, info, type)

@Suppress("FunctionName")
fun ReferenceFormulationSyntaxError(message: String, info: Origin?) =
    RmlError(message, info, RER.ReferenceFormulationSyntaxError)

@Suppress("FunctionName")
fun ReferenceFormulationExecutionError(message: String, planNode: PlanNode) =
    RmlError(message, Origin(planNode = planNode), RER.ReferenceFormulationExecutionError)

@Suppress("FunctionName")
fun RDFMappingSyntaxError(message: String, info: Origin?) = RmlError(message, info, RER.RDFMappingSyntaxError)

@Suppress("FunctionName")
fun UnsupportedMapping(message: String, info: Origin?) = RmlError(message, info, RER.UnsupportedMapping)

@Suppress("FunctionName")
fun IncorrectTermType(
    termMapName: String, currentTermtype: Resource?, validTermTypes: Set<Resource>, planNode: PlanNode
): RmlError {
    val validResources = validTermTypes.minus(BURP.CollectionOrContainer)
        .plus(if (validTermTypes.contains(RML.IRI)) RML.UnsafeIRI else null)
    val msg =
        "Incorrect term type $currentTermtype for $termMapName. Choose one of ${validResources.joinToString(", ")}"
    return RmlError(msg, Origin(planNode = planNode), RER.IncorrectTermType)
}

@Suppress("FunctionName")
fun NoTriplesMap() = RmlError(
    "No triples map (with rml:logicalSource) found in mapping.", Origin(), RER.NoTriplesMap
)

@Suppress("FunctionName")
fun UnexpectedError(ex: Exception, origin: Origin) = RmlError(
    message = ex.message ?: "Unexpected error look at stack trace.",
    origin = origin,
    errorType = RER.Error,
    exception = ex
)

@Suppress("FunctionName")
fun UnexpectedError(ex: Exception) = RmlError(
    message = ex.message ?: "Unexpected error look at stack trace.",
    origin = null,
    errorType = RER.Error,
    exception = ex
)