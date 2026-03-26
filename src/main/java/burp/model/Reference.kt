package burp.model

import burp.reporting.Origin

abstract class Reference(val reference: String?, var origin: Origin) : Expression {
    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = emptySequence()
    override fun dependencies(): Sequence<PlanNode> = emptySequence()

    // If the term map is a reference-valued term map, 
    // then the generated RDF term is determined by applying the 
    // term generation rules to its reference value.
    fun values(i: Iteration): List<Any?> {
        return getValues(i)
    }

    abstract fun getValues(i: Iteration): List<Any?>

    open fun getStrings(i: Iteration): List<String> {
        return getValues(i).mapNotNull { it?.toString() }
    }
}

// TODO: Evaluate if we can do without doing a proxy and replace it when wiring.
class RawReference(reference: String?, origin: Origin) : Reference(reference, origin) {
   var compiledReference: Reference? = null

    override fun getValues(i: Iteration): List<Any?> {
        return compiledReference!!.getValues(i)
    }

    override fun getStrings(i: Iteration): List<String> {
        return compiledReference!!.getStrings(i)
    }
}