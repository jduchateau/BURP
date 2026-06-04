package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.ReturnMap)
class ReturnMap : TermMap() {
    init {
        termType = rdfkt.NamedTerm(Rml.IRI)
    }

    override fun getName() = "return map"

    override fun getAllowedTermTypes(): Set<rdf.Term> = setOf(rdfkt.NamedTerm(Rml.IRI), rdfkt.NamedTerm(Rml.URI))
}