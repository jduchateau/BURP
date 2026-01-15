package burp.reporting

import burp.model.TriplesMap

data class RmlExecutionReport(
    val errors: MutableList<RmlError> = mutableListOf(),
    var executionPlan: List<TriplesMap> = emptyList(),
    val statistics: Statistics = Statistics()
)


data class Statistics(
    var generatedStatementPerTriplesMap: Map<TriplesMap, Long> = emptyMap(),
    var generatedStatements: Long = 0
)