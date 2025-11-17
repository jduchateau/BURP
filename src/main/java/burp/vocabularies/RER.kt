package burp.vocabularies

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

object RER {
    const val uri = "http://w3id.org/rml/report#"

    private fun resource(local: String): Resource = ResourceFactory.createResource(uri + local)
    private fun property(local: String): Property = ResourceFactory.createProperty(uri + local)

    val RmlEngineReport: Resource = resource("RmlEngineReport")
    val hasStatistics: Property = property("hasStatistics")
    val hasExecutionPlan: Property = property("hasExecutionPlan")
    val hasError: Property = property("hasError")
    val hasWarning: Property = property("hasWarning")
    val Statistics: Resource = resource("Statistics")
    val generatedStatements: Property = property("generatedStatements")
    val generatedStatementsPerTarget: Property = property("generatedStatementsPerTarget")
    val generatedStatementsPerTM: Property = property("generatedStatementsPerTM")
    val numberOfTriplesMaps: Property = property("numberOfTriplesMaps")
    val ExecutionPlan: Resource = resource("ExecutionPlan")
    val Error: Resource = resource("Error")
    val Warning: Resource = resource("Warning")
    val potentialCause: Property = property("potentialCause")
    val potentialSolution: Property = property("potentialSolution")
    val potentialConfusion: Property = property("potentialConfusion")
    val hasSourceNode: Property = property("hasSourceNode")
    val hasMappingNode: Property = property("hasMappingNode")
    val lineNumber: Property = property("lineNumber")
    val colNumber: Property = property("colNumber")
    val hasSourceData: Property = property("hasSourceData")
    val ExecutionError: Resource = resource("ExecutionError")
    val DataError: Resource = resource("DataError")
    val TypeConversionError: Resource = resource("TypeConversionError")
    val UnparseableDataError: Resource = resource("UnparseableDataError")
    val SourceAccessError: Resource = resource("SourceAccessError")
    val LogicalSourceError: Resource = resource("LogicalSourceError")
    val ReferenceFormulationExecutionError: Resource = resource("ReferenceFormulationExecutionError")
    val FunctionExecutionError: Resource = resource("FunctionExecutionError")
    val InvalidRDF: Resource = resource("InvalidRDF")
    val InvalidIRI: Resource = resource("InvalidIRI")
    val PredicateNotReifiable: Resource = resource("PredicateNotReifiable")
    val MappingError: Resource = resource("MappingError")
    val RDFMappingSyntaxError: Resource = resource("RDFMappingSyntaxError")
    val UnsupportedMapping: Resource = resource("UnsupportedMapping")
    val UnsupportedFunction: Resource = resource("UnsupportedFunction")
    val OutOfSpec: Resource = resource("OutOfSpec")
    val ReferenceFormulationSyntaxError: Resource = resource("ReferenceFormulationSyntaxError")
    val NoTriplesMap: Resource = resource("NoTriplesMap")

}