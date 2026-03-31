package burp.model

import burp.model.lv.FieldParent
import burp.reporting.BurpException
import burp.reporting.Origin

abstract class AbstractLogicalSource : Iterable, FieldParent {
    @JvmField
    var nulls = mutableSetOf<Any?>()

    override var parent: PlanNode? = null

    @Throws(BurpException::class)
    abstract fun iterator(): Iterator<Iteration>

    override val absoluteFieldName: String = "<i>"

    abstract fun sourceReference(reference: String, origin: Origin): Reference
}