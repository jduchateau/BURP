package burp.reporting

import burp.model.TriplesMap

data class RmlEngineReport(
    val errors: MutableList<Error> = mutableListOf(),
    val warnings: MutableList<Warning> = mutableListOf(),
    val executionPlan: List<TriplesMap> = emptyList(),
    val statistics: Statistics? = null
)


data class Statistics(
    var generatedStatements: Int = 0
)