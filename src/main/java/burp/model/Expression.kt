package burp.model

import burp.reporting.PlanNode
import burp.reporting.StatementParts

abstract class Expression : PlanNode{
    var origin = mutableListOf<StatementParts>()
}

