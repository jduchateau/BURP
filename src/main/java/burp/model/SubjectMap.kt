package burp.model

import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource

class SubjectMap : TermMap() {
    var classes = mutableListOf<Resource>()
    var graphMaps = mutableListOf<GraphMap>()

    init {
        termType = RML.IRI
    }

    override fun getName(): String {
        return "subject map"
    }

    override fun getAllowedTermTypes(): List<Resource> {
        return listOf(RML.IRI, RML.URI, RML.BLANKNODE)
    }

    override fun isGatherMap(): Boolean {
        return gatherMap != null
    }
}