package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfType

@RdfType("http://w3id.org/rml/InputValueMap") // Placeholder if no Rml.InputValueMap exists
class InputValueMap : TermMap() {

    init {
        termType = rdfkt.NamedTerm(Rml.Literal)
    }

    override fun getName(): String = "input value map"

    override fun getAllowedTermTypes(): Set<rdf.Term> = setOf(rdfkt.NamedTerm(Rml.IRI), rdfkt.NamedTerm(Rml.URI), rdfkt.NamedTerm(Rml.BlankNode), rdfkt.NamedTerm(Rml.Literal), rdfkt.NamedTerm(BURP.CollectionOrContainer))
}