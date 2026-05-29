package burp.model

import burp.vocabularies.RML

import org.apache.jena.rdf.model.Resource

class GraphMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "graph map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI, RML.BLANKNODE)
}