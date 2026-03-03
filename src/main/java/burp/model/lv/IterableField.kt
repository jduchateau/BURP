package burp.model.lv

import burp.ls.LogicalSourceFactory
import burp.model.LogicalSource
import org.apache.jena.rdf.model.Resource

class IterableField : Field() {
    lateinit var iterator: String
    var referenceFormulation: Resource? = null

    fun enrich(underlying: LogicalIteration): List<LogicalIteration> {
        val list = mutableListOf<LogicalIteration>()

        // The iterator has changed
        // We take the iterator from the parent
        val iterationContent = underlying.getIterationString(parent.absoluteFieldName)
        val changedIterator = LogicalSourceFactory.changeIterator(
            iterationContent!!,
            // The reference formulation has changed.
            referenceFormulation ?: ancestorReferenceFormulation!!,
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
            if (referenceFormulation != null) return referenceFormulation
            if (parent is IterableField) return (parent as IterableField).ancestorReferenceFormulation
            if (parent is LogicalSource) return (parent as LogicalSource).referenceFormulation
            return null
        }
}