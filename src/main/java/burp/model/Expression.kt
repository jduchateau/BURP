package burp.model

interface Expression : PlanNode {
    abstract override var parent: PlanNode?
}
