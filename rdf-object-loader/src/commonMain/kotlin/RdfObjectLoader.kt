package rdfobjectloader

import rdfobjectloader.model.DatasetCore
import rdfobjectloader.model.NamedNode
import rdfobjectloader.model.Term
import kotlin.reflect.KClass

interface RdfObjectLoader {

    /**
     * Maps an RDF Resource into a Kotlin object.
     * @param dataset The dataset containing the graph.
     * @param resource The subject to start mapping from.
     * @param targetClasses The candidate Kotlin classes to instantiate.
     */
    fun <T : Any> map(dataset: DatasetCore, resource: Term, targetClasses: Set<KClass<out T>>): T

    /**
     * Registers a specific type binding (Decidable Type).
     * Whenever `rdfType` is encountered as an `rdf:type` in the graph, it will be mapped to `type`.
     */
    fun addDecidableType(rdfType: NamedNode, type: KClass<*>): RdfObjectLoader

    /**
     * Binds an interface/abstract class to a specific concrete implementation class.
     */
    fun bindInterfaceImplementation(interfaze: KClass<*>, implementation: KClass<*>): RdfObjectLoader

}
