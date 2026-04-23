package burp.model

class RDFNodeConstant(var constant: Term? = null) : Expression {
    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = emptySequence()
    override fun dependencies(): Sequence<PlanNode> = emptySequence()
}