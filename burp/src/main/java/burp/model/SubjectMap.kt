package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfShortcutProperty
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.SubjectMap)
class SubjectMap : TermMap() {
    @RdfProperty(Rml.`class`)
    var classes = mutableListOf<rdf.Term>()
    @RdfProperty(Rml.graphMap)
    @RdfShortcutProperty(Rml.graph, Rml.constant)
    var graphMaps = mutableListOf<GraphMap>()

    init {
        termType = rdfkt.NamedTerm(Rml.IRI)
    }

    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(super.children())
        yieldAll(graphMaps)
    }

    override fun getName() = "subject map"

    override fun getAllowedTermTypes(): Set<rdf.Term> =
        setOf(rdfkt.NamedTerm(Rml.IRI), rdfkt.NamedTerm(Rml.URI), rdfkt.NamedTerm(Rml.BlankNode), rdfkt.NamedTerm(BURP.CollectionOrContainer.uri))

}