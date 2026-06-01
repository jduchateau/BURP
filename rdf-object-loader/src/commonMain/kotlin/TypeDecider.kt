package rdfobjectloader

import rdf.DatasetCore
import rdf.Term
import kotlin.reflect.KClass

interface TypeDecider {
    /**
     * Determines which Kotlin classes to instantiate based on the RDF dataset and the subject resource.
     */
    fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>>
}
