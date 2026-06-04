package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.FunctionMap)
class FunctionMap : TermMap() {
    init {
        termType = rdfkt.NamedTerm(Rml.IRI)
    }

    override fun getName() = "function map"

    override fun getAllowedTermTypes(): Set<rdf.Term> = setOf(Rml.IRI, Rml.URI).map { rdfkt.NamedTerm(it) }.toSet()
}