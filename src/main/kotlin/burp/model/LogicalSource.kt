package burp.model

abstract class LogicalSource : AbstractLogicalSource() {
    override fun children(): Sequence<PlanNode> = sequenceOf()
    override fun dependencies(): Sequence<PlanNode> = children()
}