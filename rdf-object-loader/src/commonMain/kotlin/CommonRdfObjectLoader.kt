package rdfobjectloader

import rdf.DatasetCore
import rdf.Term
import kotlin.reflect.KClass

interface RdfModelMapper<T : Any> {
    fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): T
}

class CommonRdfObjectLoader(
    override var logger: RdfLogger? = null
) : RdfObjectLoader {
    private val mappers = mutableMapOf<KClass<*>, RdfModelMapper<*>>()
    private val decidableTypes = mutableMapOf<String, KClass<*>>()
    private val interfaceBindings = mutableMapOf<KClass<*>, KClass<*>>()
    private val typeDeciders = mutableListOf<TypeDecider>()
    private val cache = mutableMapOf<Term, Any>()

    fun getCache(): Map<Term, Any> = cache

    fun <T : Any> registerMapper(clazz: KClass<T>, mapper: RdfModelMapper<T>): CommonRdfObjectLoader {
        mappers[clazz] = mapper
        return this
    }

    fun registerTypeDecider(decider: TypeDecider): CommonRdfObjectLoader {
        typeDeciders.add(decider)
        return this
    }

    override fun <T : Any> map(dataset: DatasetCore, resource: Term, targetClasses: Set<KClass<out T>>): T {
        if (cache.containsKey(resource)) {
            @Suppress("UNCHECKED_CAST")
            return cache[resource] as T
        }

        var concreteClass: KClass<*> = targetClasses.firstOrNull() ?: throw IllegalArgumentException("No target class provided")

        // 1. Check if the resource has an rdf:type that maps to a DecidableType
        val rdfTypes = dataset.match(subject = resource, predicate = rdfkt.NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")).map { it.`object` }.toList()
        logger?.log("Mapping resource=${resource.value} with targetClasses=${targetClasses.map { it.simpleName }.toList()}")
        logger?.log("rdfTypes found=${rdfTypes.map { it.value }.toList()}")
        logger?.log("decidableTypes keys=${decidableTypes.keys.toList()}")
        var foundDecidable = false

        for (rdfType in rdfTypes) {
            if (decidableTypes.containsKey(rdfType.value)) {
                concreteClass = decidableTypes[rdfType.value]!!
                foundDecidable = true
                break
            }
        }

        // 1.5. If not resolved by type mapping, query custom type deciders
        if (!foundDecidable) {
            for (decider in typeDeciders) {
                val decided = decider.decide(dataset, resource, concreteClass)
                if (decided.isNotEmpty()) {
                    concreteClass = decided.first()
                    break
                }
            }
        }

        // 2. Resolve interface bindings
        concreteClass = interfaceBindings[concreteClass] ?: concreteClass

        // 3. Get the registered mapper for the concrete class
        val mapper = mappers[concreteClass] ?: throw IllegalArgumentException("No mapper registered for class ${concreteClass.simpleName ?: concreteClass.toString()}")

        @Suppress("UNCHECKED_CAST")
        val instance = (mapper as RdfModelMapper<T>).map(dataset, resource, this, cache)
        
        return instance
    }

    override fun addDecidableType(rdfType: String, type: KClass<*>): RdfObjectLoader {
        decidableTypes[rdfType] = type
        return this
    }



    override fun bindInterfaceImplementation(interfaze: KClass<*>, implementation: KClass<*>): RdfObjectLoader {
        interfaceBindings[interfaze] = implementation
        return this
    }
}
