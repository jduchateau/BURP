package burp.model

import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource

class FunctionMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "function map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI)
}