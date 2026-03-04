package burp.model

import burp.reporting.Origin

class Reference(reference: String?) : Expression() {
    var reference: String? = null

    init {
        this.reference = reference
    }

    // If the term map is a reference-valued term map, 
    // then the generated RDF term is determined by applying the 
    // term generation rules to its reference value.
    fun values(i: Iteration): List<Any?> {
        return i.getValuesFor(reference, Origin(this, origin))
    }
}