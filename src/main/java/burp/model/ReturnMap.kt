package burp.model

import burp.vocabularies.RML

class ReturnMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "return map"

    override fun getAllowedTermTypes() = listOf(RML.IRI, RML.URI)

    override fun isGatherMap() = false
}