package burp.model.lv

import burp.ls.LogicalSourceFactory
import burp.model.Iterable
import burp.model.LogicalSource
import org.apache.jena.rdf.model.Resource

class IterableField : Field(), Iterable {
    // Reference formulation may have default iterator (e.g. CSV)
    var iterator: String? = null

    override var referenceFormulation: Resource
        get() = declaredReferenceFormulation ?: ancestorReferenceFormulation!!
        set(value) {declaredReferenceFormulation = value}

    var declaredReferenceFormulation: Resource? = null

    fun enrich(underlying: LogicalIteration): List<LogicalIteration> {
        val list = mutableListOf<LogicalIteration>()

        // The iterator has changed
        // We take the iterator from the parent
        val iterationContent = underlying.getIterationString(parent.absoluteFieldName)
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
            if (parent is IterableField) return (parent as IterableField).ancestorReferenceFormulation
            if (parent is LogicalSource) return (parent as LogicalSource).referenceFormulation
            return null
        }
}