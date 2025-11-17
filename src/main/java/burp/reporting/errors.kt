package burp.reporting

import burp.vocabularies.RER
import org.apache.jena.rdf.model.Resource
import turtleprov.kotlin.NodeInfo


data class TracingInfo(
    val file: String,
    val location: NodeInfo?,
    val sourceNode: Resource?,
) {
    fun fileLocationString(): String {
        val locationStr = location?.let {
            val start = "${it.start?.line ?: '?'}:${it.start?.column ?: '?'}"
            val end = it.end?.let { end -> "${end.line}:${end.column}" }
            if (end != null) "$start-$end" else start
        } ?: ""

        return if (locationStr.isNotEmpty()) "$file:$locationStr" else file
    }
}

sealed class Report(
    open val message: String,
    open val tracingInfo: TracingInfo?
)

class Warning(
    override val message: String,
    override val tracingInfo: TracingInfo?
) : Report(message, tracingInfo)

sealed class Error(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    open val errorType: Resource
) : Report(message, tracingInfo)

class UnexpectedError(
    val ex: Exception
) : Error(ex.message ?: "Unexpected error look at stack trace.", null, RER.Error)

class BurpException(val error: Error) : Exception(error.message)

open class ExecutionError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : Error(message, tracingInfo, errorType)

open class DataError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : ExecutionError(message, tracingInfo, errorType)

class TypeConversionError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : DataError(message, tracingInfo, errorType)

class UnparseableDataError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : DataError(message, tracingInfo, errorType)

class SourceAccessError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : ExecutionError(message, tracingInfo, errorType)

class LogicalSourceError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : ExecutionError(message, tracingInfo, errorType)

class ReferenceFormulationExecutionError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : ExecutionError(message, tracingInfo, errorType)

class FunctionExecutionError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : ExecutionError(message, tracingInfo, errorType)

open class InvalidRDF(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : ExecutionError(message, tracingInfo, errorType)

class InvalidIRI(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : InvalidRDF(message, tracingInfo, errorType)

class PredicateNotReifiable(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : InvalidRDF(message, tracingInfo, errorType)

open class MappingError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : Error(message, tracingInfo, errorType)

class RDFMappingSyntaxError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : MappingError(message, tracingInfo, errorType)

open class UnsupportedMapping(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : MappingError(message, tracingInfo, errorType)

class UnsupportedFunction(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : UnsupportedMapping(message, tracingInfo, errorType)

class OutOfSpec(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : MappingError(message, tracingInfo, errorType)

class ReferenceFormulationSyntaxError(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : MappingError(message, tracingInfo, errorType)

class NoTriplesMap(
    override val message: String,
    override val tracingInfo: TracingInfo?,
    override val errorType: Resource
) : MappingError(message, tracingInfo, errorType)


