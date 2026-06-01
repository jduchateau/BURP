package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.RdfType

@RdfType("http://w3id.org/rml/InputValueMap") // Placeholder if no Rml.InputValueMap exists
class InputValueMap : TermMap() {

    init {
        termType = RML.LITERAL
    }

    override fun getName(): String = "input value map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL, BURP.CollectionOrContainer)
}