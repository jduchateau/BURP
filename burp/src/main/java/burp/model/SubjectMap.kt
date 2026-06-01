package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.RML
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.SubjectMap)
class SubjectMap : TermMap() {
    @RdfProperty(Rml.`class`)
    var classes = mutableListOf<Resource>()
    @RdfProperty(Rml.graphMap)
    var graphMaps = mutableListOf<GraphMap>()

    init {
        termType = RML.IRI
    }

    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(super.children())
        yieldAll(graphMaps)
    }

    override fun getName() = "subject map"

    override fun getAllowedTermTypes(): Set<Resource> =
        setOf(RML.IRI, RML.URI, RML.BLANKNODE, BURP.CollectionOrContainer)

}