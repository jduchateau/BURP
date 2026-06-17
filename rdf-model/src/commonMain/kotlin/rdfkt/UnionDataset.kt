package rdfkt

import rdf.DatasetCore
import rdf.Quad
import rdf.Term

class UnionDataset(
    private val wrapped: DatasetCore,
    private val additionalQuads: MutableSet<Quad> = mutableSetOf()
) : DatasetCore {
    override val size: Int
        get() = (wrapped.asSequence() + additionalQuads.asSequence()).distinct().count()

    override fun add(quad: Quad): DatasetCore {
        additionalQuads.add(quad)
        return this
    }

    override fun delete(quad: Quad): DatasetCore {
        additionalQuads.remove(quad)
        return this
    }

    override fun has(quad: Quad): Boolean {
        return additionalQuads.contains(quad) || wrapped.has(quad)
    }

    override fun match(subject: Term?, predicate: Term?, `object`: Term?, graph: Term?): DatasetCore {
        val matchedWrapped = wrapped.match(subject, predicate, `object`, graph)
        val matchedAdditional = additionalQuads.filter { quad ->
            (subject == null || quad.subject.value == subject.value && quad.subject.termType == subject.termType) &&
            (predicate == null || quad.predicate.value == predicate.value && quad.predicate.termType == predicate.termType) &&
            (`object` == null || quad.`object`.value == `object`.value && quad.`object`.termType == `object`.termType) &&
            (graph == null || quad.graph.value == graph.value && quad.graph.termType == graph.termType)
        }
        return UnionDataset(matchedWrapped, matchedAdditional.toMutableSet())
    }

    override fun iterator(): Iterator<Quad> {
        return (wrapped.asSequence() + additionalQuads.asSequence()).distinct().iterator()
    }
}
