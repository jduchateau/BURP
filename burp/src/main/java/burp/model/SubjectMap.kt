package burp.model

import burp.vocabularies.BURP
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource

class SubjectMap : TermMap() {
    var classes = mutableListOf<Resource>()
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