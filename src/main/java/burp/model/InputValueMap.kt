package burp.model

import burp.vocabularies.RML

class InputValueMap : TermMap() {

    init {
        termType = RML.LITERAL
    }

    override fun getName(): String = "input value map"

    override fun getAllowedTermTypes()=listOf(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL)

    override fun isGatherMap() = false
}