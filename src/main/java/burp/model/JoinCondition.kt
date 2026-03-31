package burp.model

class JoinCondition : PlanNode, ReferenceHolder {
    lateinit var parentMap: ConcreteExpressionMap
    lateinit var childMap: ConcreteExpressionMap

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (::parentMap.isInitialized) yield(parentMap)
        if (::childMap.isInitialized) yield(childMap)
    }
    override fun dependencies(): Sequence<PlanNode> = children()

    override fun compileReferences() {
        val parentScope = ancestor<ParentJoinReferenceScope>()!!
        for (ref in parentMap.descendants<RawReference>()) {
            if (ref.reference != null && ref.compiledReference == null) {
                ref.compiledReference = parentScope.buildParentJoinReference(ref.reference, ref.origin)
            }
        }
        val localScope = ancestor<LocalReferenceScope>()!!
        for (ref in childMap.descendants<RawReference>()) {
            if (ref.reference != null && ref.compiledReference == null) {
                ref.compiledReference = localScope.buildLocalReference(ref.reference, ref.origin)
            }
        }
    }
}