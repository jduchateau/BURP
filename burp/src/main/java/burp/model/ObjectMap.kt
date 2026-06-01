package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.RML
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.RdfType

sealed interface BaseObjectMap

@RdfType(Rml.ObjectMap)
class ObjectMap : TermMap(), BaseObjectMap {
    init {
        termType = RML.IRI
    }

    override fun getName() = "object map"

    override fun getAllowedTermTypes(): Set<Resource> = setOf(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL, BURP.CollectionOrContainer)
}