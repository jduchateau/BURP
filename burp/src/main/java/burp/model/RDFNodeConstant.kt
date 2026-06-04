package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.MappedByPredicate
import rdfobjectloader.annotations.RdfId

@MappedByPredicate(Rml.constant)
class RDFNodeConstant(@RdfId var constant: rdf.Term? = null) : Expression {
    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = emptySequence()
    override fun dependencies(): Sequence<PlanNode> = emptySequence()
}