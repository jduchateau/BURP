package rdfkt

import rdf.DatasetCore
import rdf.Quad
import rdf.Term

@Deprecated("Prefer using platform specific implementations, at least when performance matters.")
class InMemoryDatasetCore(
    private val quads: MutableSet<Quad> = mutableSetOf()
) : DatasetCore {
    override val size: Int
        get() = quads.size

    override fun add(quad: Quad): DatasetCore {
        quads.add(quad)
        return this
    }

    override fun delete(quad: Quad): DatasetCore {
        quads.remove(quad)
        return this
    }

    override fun has(quad: Quad): Boolean {
        return quads.contains(quad)
    }

    override fun match(subject: Term?, predicate: Term?, `object`: Term?, graph: Term?): DatasetCore {
        val filtered = quads.filter { quad ->
            (subject == null || quad.subject.value == subject.value && quad.subject.termType == subject.termType) &&
            (predicate == null || quad.predicate.value == predicate.value && quad.predicate.termType == predicate.termType) &&
            (`object` == null || quad.`object`.value == `object`.value && quad.`object`.termType == `object`.termType) &&
            (graph == null || quad.graph.value == graph.value && quad.graph.termType == graph.termType)
        }
        return InMemoryDatasetCore(filtered.toMutableSet())
    }

    override fun iterator(): Iterator<Quad> {
        return quads.iterator()
    }
}
