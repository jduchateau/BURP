package burp.reporting

import burp.model.TriplesMap

data class RmlEngineReport(
    val errors: MutableList<RmlError> = mutableListOf(),
    val warnings: MutableList<Warning> = mutableListOf(),
    var executionPlan: List<TriplesMap> = emptyList(),
    val statistics: Statistics? = null
)


data class Statistics(
    var generatedStatements: Int = 0
)