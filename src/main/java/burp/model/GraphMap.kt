package burp.model

import burp.vocabularies.RML

class GraphMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "graph map"

    override fun getAllowedTermTypes() = listOf(RML.IRI, RML.URI, RML.BLANKNODE)

    override fun isGatherMap() = false
}