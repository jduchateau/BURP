package burp.reporting

import burp.vocabularies.RER
import org.apache.jena.ontology.OntClass
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import turtleprov.kotlin.JenaConverter
import turtleprov.kotlin.NodeInfo
import java.nio.file.Path


interface PlanNode

enum class StatementPart {
    Subject, Predicate, Object
}
data class StatementParts(val stmt: Statement, val subject: Boolean, val predicate: Boolean, val `object`: Boolean){
    companion object {
        fun from(stmt: Statement, vararg parts: StatementPart): StatementParts {
            return StatementParts(stmt, StatementPart.Subject in parts, StatementPart.Predicate in parts, StatementPart.Object in parts)
        }
        fun fromPredicateObject(stmt: Statement): StatementParts = StatementParts(
            stmt,
            subject = false,
            predicate = true,
            `object` = true
        )

        fun fromObject(stmt: Statement): StatementParts = StatementParts(
            stmt,
            subject = false,
            predicate = false,
            `object` = true
        )
    }
}

data class Origin(
    /// The plan node in which the issue occurred, if exists (during parsing we may not plan nodes).
    val planNode: PlanNode? = null,
    // Statements at the source of the issue, if exists
    // (as soon as we have an RDF graph, we should have statements to pinpoints)
    val sourceStatements: List<StatementParts>? = null,
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

    fun locations(): List<NodeInfo> {
        val converter = JenaConverter()
        if (sourceStatements.isNullOrEmpty()) return emptyList()
        val locations = sourceStatements
            .map {
                val infos = converter.fromAnnotations(it.stmt)
                listOfNotNull(
                    if (it.subject) infos.subjectInfo else null,
                    if (it.predicate) infos.predicateInfo else null,
                    if (it.`object`) infos.objectInfo else null
                )
            }
            .flatten()
        return locations
    }
}

fun fileLocationString(file: Path, location: NodeInfo?): String {
    val locationStr = location?.let {
        val start = "${it.start?.line ?: '?'}:${it.start?.column ?: '?'}"
        val end = it.end?.let { end -> "${end.line}:${end.column}" }
        if (end != null) "$start-$end" else start
    } ?: ""

    return if (locationStr.isNotEmpty()) "$file:$locationStr" else file.toString()
}

sealed class Report(
    open val message: String,
    open val origin: Origin?,
    open val errorType: OntClass,
    open val exception: Exception? = null
)

class Warning(
    override val message: String,
    override val origin: Origin?
) : Report(message, origin, RER.Warning)

class RmlError(
    override val message: String,
    override val origin: Origin?,
    override val errorType: OntClass,
    override val exception: Exception? = null
) : Report(message, origin, errorType, exception) {

    init {
        assert(errorType.hasSuperClass(RER.Error)) { "$errorType is not a subClass of ${RER.Error}" }
    }

    @Suppress("FunctionName")
    companion object {

        fun ExecutionError(message: String, info: Origin?, type: OntClass) = RmlError(message, info, type)

        fun DataError(message: String, info: Origin?, type: OntClass) =
            RmlError(message, info, type)

        fun TypeConversionError(message: String, info: Origin?) =
            RmlError(message, info, RER.TypeConversionError) // Uses schemagen constant

        fun UnparseableDataError(message: String, info: Origin?) =
            RmlError(message, info, RER.UnparseableDataError)

        fun SourceAccessError(message: String, info: Origin?, ex: Exception?) =
            RmlError(message, info, RER.SourceAccessError, ex)

        fun LogicalSourceError(message: String, info: Origin?) =
            RmlError(message, info, RER.LogicalSourceError)

        fun ReferenceFormulationExecutionError(message: String, planNode: PlanNode) =
            RmlError(message, Origin(planNode = planNode), RER.ReferenceFormulationExecutionError)

        fun FunctionExecutionError(message: String, info: Origin?, type: OntClass) =
            RmlError(message, info, type)


        fun InvalidRDF(message: String, info: Origin?, type: OntClass) =
            RmlError(message, info, type)

        fun InvalidIRI(message: String, info: Origin?) =
            RmlError(message, info, RER.InvalidIRI)

        fun PredicateNotReifiable(message: String, info: Origin?) =
            RmlError(message, info, RER.PredicateNotReifiable)


        fun RDFMappingSyntaxError(message: String, info: Origin?) =
            RmlError(message, info, RER.RDFMappingSyntaxError)

        fun UnsupportedMapping(message: String, info: Origin?) =
            RmlError(message, info, RER.UnsupportedMapping)

        fun UnsupportedFunction(message: String, info: Origin?) =
            RmlError(message, info, RER.UnsupportedFunction)

        fun ReferenceFormulationSyntaxError(message: String, info: Origin?) =
            RmlError(message, info, RER.ReferenceFormulationSyntaxError)


        fun IncorrectTermType(
            termMapName: String,
            currentTermtype: Resource,
            validTermTypes: List<Resource>,
            planNode: PlanNode
        ): RmlError {
            val msg = "Incorrect term type $currentTermtype for $termMapName. " +
                    "Choose one of ${validTermTypes.joinToString(", ")}"
            return RmlError(msg, Origin(planNode = planNode), RER.IncorrectTermType)
        }

        fun NoTriplesMap() =
            RmlError(
                "No triples map (with rml:logicalSource) found in mapping.",
                Origin(),
                RER.NoTriplesMap
            )

        fun UnexpectedError(ex: Exception, planNode: PlanNode?) =
            RmlError(
                message = ex.message ?: "Unexpected error look at stack trace.",
                origin = planNode?.let { Origin(planNode = it) },
                errorType = RER.Error,
                exception = ex
            )
    }
}


class BurpException(val error: RmlError) : RuntimeException(error.message)



