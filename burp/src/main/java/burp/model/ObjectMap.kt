package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfType

sealed interface BaseObjectMap : PlanNode

@RdfType(Rml.ObjectMap)
class ObjectMap : TermMap(), BaseObjectMap {
    init {
        termType = rdfkt.NamedTerm(Rml.IRI)
    }

    override fun getName() = "object map"

    override fun getAllowedTermTypes(): Set<rdf.Term> = setOf(rdfkt.NamedTerm(Rml.IRI), rdfkt.NamedTerm(Rml.URI), rdfkt.NamedTerm(Rml.BlankNode), rdfkt.NamedTerm(Rml.Literal), rdfkt.NamedTerm(BURP.CollectionOrContainer))
}