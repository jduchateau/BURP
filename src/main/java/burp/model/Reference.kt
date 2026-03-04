package burp.model

import burp.reporting.Origin

class Reference(val reference: String?, val origin: Origin) : Expression {
    // If the term map is a reference-valued term map, 
    // then the generated RDF term is determined by applying the 
    // term generation rules to its reference value.
    fun values(i: Iteration): List<Any?> {
        return i.getValuesFor(reference, origin)
    }
}