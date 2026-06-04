package burp.model.deciders

import burp.model.ObjectMap
import burp.model.ReferencingObjectMap
import burp.vocabularies.Rml
import rdf.DatasetCore
import rdf.Term
import rdfobjectloader.TypeDecider
import kotlin.reflect.KClass

class ObjectMapTypeDecider : TypeDecider {
    override fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>> {
        if (targetClass != burp.model.BaseObjectMap::class && 
            targetClass != burp.model.ObjectMap::class && 
            targetClass != burp.model.TermGenerator::class) return emptySet()
        val parentTriplesMapProp = rdfkt.NamedTerm(Rml.parentTriplesMap)
        
        if (dataset.match(subject = resource, predicate = parentTriplesMapProp).any()) {
            return setOf(ReferencingObjectMap::class)
        }
        return setOf(ObjectMap::class)
    }
}
