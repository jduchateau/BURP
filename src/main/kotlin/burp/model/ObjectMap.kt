package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource

class ObjectMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "object map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL, BURP.CollectionOrContainer)
}