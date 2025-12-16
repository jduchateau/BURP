package burp.reporting

import burp.model.TriplesMap

data class RmlExecutionReport(
    val errors: MutableList<RmlError> = mutableListOf(),
    var executionPlan: List<TriplesMap> = emptyList(),
    val statistics: Statistics? = null
)


data class Statistics(
    var generatedStatements: Int = 0
)