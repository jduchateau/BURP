package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.GraphMap)
class GraphMap : TermMap() {
    init {
        termType = rdfkt.NamedTerm(Rml.IRI)
    }

    override fun getName() = "graph map"

    override fun getAllowedTermTypes(): Set<rdf.Term> = setOf(rdfkt.NamedTerm(Rml.IRI), rdfkt.NamedTerm(Rml.URI), rdfkt.NamedTerm(Rml.BlankNode))
}