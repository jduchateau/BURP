package burp.model.deciders

import burp.model.ObjectMap
import burp.model.ReferencingObjectMap
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.ResourceFactory
import rdf.DatasetCore
import rdf.Term
import rdfkt.JenaDataset
import rdfkt.JenaNamedNode
import rdfobjectloader.TypeDecider
import kotlin.reflect.KClass

class ObjectMapTypeDecider : TypeDecider {
    override fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>> {
        if (targetClass != burp.model.BaseObjectMap::class && targetClass != burp.model.ObjectMap::class) return emptySet()
        val model = (dataset as JenaDataset).model
        val jenaResource = (resource as JenaNamedNode).node
        val parentTriplesMapProp = ResourceFactory.createProperty(Rml.parentTriplesMap)
        
        if (model.contains(jenaResource, parentTriplesMapProp)) {
            return setOf(ReferencingObjectMap::class)
        }
        return setOf(ObjectMap::class)
    }
}
