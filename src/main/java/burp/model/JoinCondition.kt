package burp.model

class JoinCondition : PlanNode {
    lateinit var parentMap: ConcreteExpressionMap
    lateinit var childMap: ConcreteExpressionMap

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (::parentMap.isInitialized) yield(parentMap)
        if (::childMap.isInitialized) yield(childMap)
    }
    override fun dependencies(): Sequence<PlanNode> = children()
}