package burp.model.deciders

import rdf.DatasetCore
import rdf.Term
import rdfobjectloader.TypeDecider
import kotlin.reflect.KClass

class ExpressionTypeDecider : TypeDecider {
    override fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>> {
        if (targetClass != burp.model.Expression::class) return emptySet()
        val stmts = dataset.match(resource, null, null).toList()
        
        // Very basic checks
        if (stmts.any { it.`object`.value == "http://w3id.org/rml/FunctionExecution" }) return setOf(burp.model.FunctionExecution::class)
        if (stmts.any { it.predicate.value == "http://w3id.org/rml/template" }) return setOf(burp.model.Template::class)
        if (stmts.any { it.predicate.value == "http://w3id.org/rml/reference" }) return setOf(burp.model.RawReference::class)
        if (stmts.any { it.predicate.value == "http://w3id.org/rml/constant" }) return setOf(burp.model.RDFNodeConstant::class)
        if (stmts.any { it.predicate.value == "http://w3id.org/rml/parentTermMap" }) return setOf(burp.model.RawReference::class) // parent reference
        if (stmts.any { it.predicate.value == "http://w3id.org/rml/joinCondition" }) return setOf(burp.model.RawReference::class) // join reference
        
        // If has language or datatype, it's a constant
        if (stmts.any { it.predicate.value == "http://w3id.org/rml/language" || it.predicate.value == "http://w3id.org/rml/datatype" }) {
            return setOf(burp.model.RDFNodeConstant::class)
        }
        
        return setOf(burp.model.Expression::class)
    }
}
