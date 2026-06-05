package burp.model

import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource

class ParameterMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "parameter map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI)
}