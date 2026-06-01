package burp.model

import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.RdfType

@RdfType("http://w3id.org/rml/ParameterMap") // Assuming ParameterMap might not have a direct Rml constant
class ParameterMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "parameter map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI)
}