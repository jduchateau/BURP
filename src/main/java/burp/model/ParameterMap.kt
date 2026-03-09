package burp.model

import burp.vocabularies.RML

class ParameterMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "parameter map"

    override fun getAllowedTermTypes() = listOf(RML.IRI, RML.URI)

    override fun isGatherMap(): Boolean = false
}