package burp.model.lv

import burp.ls.LogicalSourceFactory
import burp.model.Iterable
import burp.model.LogicalSource
import burp.model.PlanNode
import burp.model.Reference
import burp.reporting.Origin
import org.apache.jena.rdf.model.Resource

class IterableField : Field(), Iterable {
    override var parent: PlanNode? = null

    override fun children(): Sequence<PlanNode> = emptySequence()
    override fun dependencies(): Sequence<PlanNode> = emptySequence()

    // Reference formulation may have a default iterator (e.g. CSV)
    var iterator: String? = null

    override var referenceFormulation: Resource
        get() = declaredReferenceFormulation ?: ancestorReferenceFormulation!!
        set(value) {declaredReferenceFormulation = value}

    var declaredReferenceFormulation: Resource? = null

    override fun buildReference(reference: String, origin: Origin): Reference {
        TODO("Not yet implemented")
    }

    fun enrich(underlying: LogicalIteration): List<LogicalIteration> {
        val list = mutableListOf<LogicalIteration>()

        // The iterator has changed
        // We take the iterator from the parent
        val iterationContent = underlying.getIterationString(parentField.absoluteFieldName)
        val changedIterator = LogicalSourceFactory.changeIterator(
            iterationContent!!,
            // The reference formulation has changed.
            declaredReferenceFormulation ?: ancestorReferenceFormulation!!,
            iterator
        )
        changedIterator.forEachIndexed { index, iteration ->
            val e = underlying.copy()
            e.put("$absoluteFieldName.#", index)
            e.put(absoluteFieldName, iteration)
            list.add(e)

        }

        return expand(list, expressionFields, iterableFields)
    }

    private val ancestorReferenceFormulation: Resource?
        get() {
            // Since we explicitly created an IterableField for the rood. the two lines below should suffice.
            if (declaredReferenceFormulation != null) return declaredReferenceFormulation
            if (parentField is IterableField) return (parentField as IterableField).ancestorReferenceFormulation
            if (parentField is LogicalSource) return (parentField as LogicalSource).referenceFormulation
            return null
        }
}