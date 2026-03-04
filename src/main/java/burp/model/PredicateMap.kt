package burp.model

import burp.vocabularies.RML

class PredicateMap : TermMap() {
    init {
        termType = RML.IRI
    }

    override fun getName() = "predicate map"

    override fun getAllowedTermTypes() = listOf(RML.IRI, RML.URI)

    override fun isGatherMap() = false
}