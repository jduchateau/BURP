package burp.model

import burp.reporting.Origin

interface ReferenceFormulationScope : PlanNode {
    fun buildReference(reference: String, origin: Origin): Reference
}