package burp.model

import burp.vocabularies.RML

class ObjectMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "object map"

    override fun getAllowedTermTypes() = listOf(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL)

    override fun isGatherMap() = gatherMap != null
}