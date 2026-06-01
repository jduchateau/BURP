package burp.model

import burp.vocabularies.RML
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.FunctionMap)
class FunctionMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "function map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI)
}