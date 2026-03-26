package burp.model

import burp.model.lv.FieldParent
import burp.reporting.BurpException

abstract class AbstractLogicalSource : Iterable, FieldParent {
    @JvmField
    var nulls = mutableSetOf<Any?>()

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = emptySequence()
    override fun dependencies(): Sequence<PlanNode> = emptySequence()

    @Throws(BurpException::class)
    abstract fun iterator(): Iterator<Iteration>

    override val absoluteFieldName: String = "<i>"
}