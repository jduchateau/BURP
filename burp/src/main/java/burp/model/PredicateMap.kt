package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.PredicateMap)
class PredicateMap : TermMap() {
    init {
        termType = rdfkt.NamedTerm(Rml.IRI)
    }

    override fun getName() = "predicate map"

    override fun getAllowedTermTypes(): Set<rdf.Term> = setOf(rdfkt.NamedTerm(Rml.IRI), rdfkt.NamedTerm(Rml.URI))
}