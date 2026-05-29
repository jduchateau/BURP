package burp.reporting

import burp.model.MappingDocument
import burp.model.TriplesMap

data class RmlExecutionReport(
    val errors: MutableList<RmlError> = mutableListOf(),
    var executionPlan: MappingDocument? = null,
    val statistics: Statistics = Statistics()
)


data class Statistics(
    var generatedStatementPerTriplesMap: Map<TriplesMap, Long> = emptyMap(),
    var generatedStatements: Long = 0
)