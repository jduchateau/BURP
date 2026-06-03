package rdfobjectloader

import rdf.*
import rdfkt.BlankNodeOrIRI
import rdfkt.BlankTerm
import rdfkt.Graph
import rdfkt.NamedTerm
import rdfkt.Literal as LiteralKt
import rdfkt.Quad as QuadKt

fun mapRdfTermToRdfkt(t: Term): rdfkt.Term {
    return when (t) {
        is rdfkt.Term -> t
        is NamedNode -> NamedTerm(t.value)
        is BlankNode -> BlankTerm(t.value)
        is Literal -> LiteralKt(
            value = t.value,
            type = NamedTerm(t.datatype.value),
            lang = t.language.ifEmpty { null }
        )

        is DefaultGraph -> rdfkt.DefaultGraph
        is Quad -> QuadKt(
            s = mapRdfTermToRdfkt(t.subject) as BlankNodeOrIRI,
            p = mapRdfTermToRdfkt(t.predicate) as NamedTerm,
            o = mapRdfTermToRdfkt(t.`object`),
            g = mapRdfTermToRdfkt(t.graph) as Graph
        )

        else -> throw IllegalArgumentException("Unknown term: $t")
    }
}