package burp.reporting

import burp.vocabularies.RER
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntProperty
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import turtleprov.Point
import java.nio.file.Path


interface PlanNode

enum class StatementPart {
    Subject, Predicate, Object
}

sealed interface RDFGraphPointer {
    val stmt: Statement
}

data class StatementParts(
    override val stmt: Statement, val subject: Boolean, val predicate: Boolean, val `object`: Boolean
) : RDFGraphPointer {
    companion object {
        fun from(stmt: Statement, vararg parts: StatementPart): StatementParts {
            return StatementParts(
                stmt, StatementPart.Subject in parts, StatementPart.Predicate in parts, StatementPart.Object in parts
            )
        }

        fun fromPredicateObject(stmt: Statement): StatementParts = StatementParts(
            stmt, subject = false, predicate = true, `object` = true
        )
    }
}

data class LiteralPart(override val stmt: Statement, val objectRange: PointRange) : RDFGraphPointer {
    init {
        if (!stmt.`object`.isLiteral) throw IllegalArgumentException("Statement object is not a literal: $stmt")
    }
}

data class PointRange(val start: Point, val end: Point? = null) {
    operator fun plus(other: PointRange): PointRange {
        return PointRange(
            start = start + other.start,
            end = (end ?: Point.zero()) + (other.end ?: Point.zero())
        )
    }
}

data class Origin(
    /// The plan node in which the issue occurred, if exists (during parsing we may not plan nodes).
    val planNode: PlanNode? = null,
    // Statements at the source of the issue, if exists
    // (as soon as we have an RDF graph, we should have statements to pinpoints)
    val sourceStatements: List<RDFGraphPointer>? = null,
) {
    constructor(stmt: Statement, vararg stmtParts: StatementPart) : this(
        sourceStatements = listOf(
            StatementParts(
                stmt,
                subject = StatementPart.Subject in stmtParts,
                predicate = StatementPart.Predicate in stmtParts,
                `object` = StatementPart.Object in stmtParts
            )
        )
    )

    constructor(planNode: PlanNode, stmt: Statement, vararg stmtParts: StatementPart) : this(
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
    termMapName: String, currentTermtype: Resource, validTermTypes: List<Resource>, planNode: PlanNode
): RmlError {
    val msg = "Incorrect term type $currentTermtype for $termMapName. " + "Choose one of ${
        validTermTypes.joinToString(", ")
    }"
    return RmlError(msg, Origin(planNode = planNode), RER.IncorrectTermType)
}

@Suppress("FunctionName")
fun NoTriplesMap() = RmlError(
    "No triples map (with rml:logicalSource) found in mapping.", Origin(), RER.NoTriplesMap
)

@Suppress("FunctionName")
fun UnexpectedError(ex: Exception, planNode: PlanNode) = RmlError(
    message = ex.message ?: "Unexpected error look at stack trace.",
    origin = Origin(planNode = planNode),
    errorType = RER.Error,
    exception = ex
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