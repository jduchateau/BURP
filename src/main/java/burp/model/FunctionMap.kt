package burp.model

import burp.vocabularies.RML

class FunctionMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "function map"

    override fun getAllowedTermTypes() = listOf(RML.IRI, RML.URI)

    override fun isGatherMap() = false
}