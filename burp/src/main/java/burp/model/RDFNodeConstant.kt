package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.MappedByPredicate
import rdfobjectloader.annotations.RdfProperty

@MappedByPredicate(Rml.constant)
class RDFNodeConstant(@RdfProperty(Rml.constant) var constant: Term? = null) : Expression {
    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = emptySequence()
    override fun dependencies(): Sequence<PlanNode> = emptySequence()
}