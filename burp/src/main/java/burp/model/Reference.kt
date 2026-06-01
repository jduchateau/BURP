package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.PointRange
import rdfobjectloader.RDFPointer
import rdfobjectloader.annotations.MappedByPredicate
import rdfobjectloader.annotations.RdfLiteral

abstract class Reference(
    val reference: String?,
    var origin: RDFPointer
) : Expression {
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
@MappedByPredicate(Rml.reference)
class RawReference(
    @RdfLiteral
    val ref: String?,
    var refOrigin: RDFPointer
) : Reference(ref, refOrigin), ReferenceHolder {
    var compiledReference: Reference? = null

    override fun nodeRanges(): List<PointRange> {
        val pointers = origin ?: return emptyList()
        return turtleprov.retrieveTurtleLocation(listOf(pointers))
    }

    override fun getValues(i: Iteration): List<Any?> {
        return compiledReference!!.getValues(i)
    }

    override fun compileReferences() {
        if (compiledReference == null && reference != null) {
            val scope = ancestor<LocalReferenceScope>()
            if (scope != null) {
                compiledReference = scope.buildLocalReference(reference, origin)
            }
        }
    }

    override fun getStrings(i: Iteration): List<String> {
        return compiledReference!!.getStrings(i)
    }
}