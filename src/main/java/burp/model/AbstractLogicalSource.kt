package burp.model

import burp.model.lv.FieldParent
import burp.reporting.BurpException

abstract class AbstractLogicalSource : Iterable, FieldParent, ExportedReferenceScope, LogicalTargetScope {
    @JvmField
    var nulls = mutableSetOf<Any?>()
    override val logicalTargets: MutableSet<LogicalTarget> = mutableSetOf()

    override var parent: PlanNode? = null

    @Throws(BurpException::class)
    abstract fun iterator(): Iterator<Iteration>

    override val absoluteFieldName: String = "<i>"

}