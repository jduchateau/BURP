package rdfobjectloader

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.*
import rdfobjectloader.model.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

class JenaRdfObjectLoader : RdfObjectLoader {

    private val decidableTypes = mutableMapOf<String, KClass<*>>()
    private val interfaceBindings = mutableMapOf<KClass<*>, KClass<*>>()
    private val cache = mutableMapOf<Term, Any>()

    override fun <T : Any> map(dataset: DatasetCore, resource: Term, targetClasses: Set<KClass<out T>>): T {
        return mapInternal(dataset, resource, targetClasses, null)
    }

    private fun <T : Any> mapInternal(dataset: DatasetCore, resource: Term, targetClasses: Set<KClass<out T>>, triggeringQuad: Quad?): T {
        if (cache.containsKey(resource)) {
            @Suppress("UNCHECKED_CAST")
            return cache[resource] as T
        }

        // Find the actual class to instantiate
        var concreteClass: KClass<*> = targetClasses.firstOrNull() ?: throw IllegalArgumentException("No target class provided")
        
        // 1. Check if the resource has an rdf:type that maps to a DecidableType
        val rdfTypes = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.vocabulary.RDF.type)).map { it.`object` as NamedNode }.toList()
        for (rdfType in rdfTypes) {
            if (decidableTypes.containsKey(rdfType.value)) {
                concreteClass = decidableTypes[rdfType.value]!!
                break
            }
        }

        // 2. Resolve interface bindings
        if (concreteClass.isAbstract || concreteClass.java.isInterface) {
            concreteClass = interfaceBindings[concreteClass] ?: throw IllegalArgumentException("Cannot instantiate abstract class or interface: $concreteClass")
        }

        @Suppress("UNCHECKED_CAST")
        val constructor = concreteClass.primaryConstructor as KFunction<T>? ?: throw IllegalArgumentException("No primary constructor found for $concreteClass")

        val args = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            if (param.hasAnnotation<OriginQuad>()) {
                args[param] = triggeringQuad
                continue
            }

            val originOfProp = param.findAnnotation<OriginOfProperty>()
            if (originOfProp != null) {
                val quad = getOriginOfPropertyQuad(dataset, resource, concreteClass, originOfProp.propertyName)
                if (quad != null) {
                    args[param] = StatementParts(quad, subject = false, predicate = false, `object` = true)
                }
                continue
            }

            if (param.hasAnnotation<RdfLiteral>()) {
                args[param] = mapValue(dataset, resource, param.type.jvmErasure, param, triggeringQuad)
                continue
            }

            if (param.hasAnnotation<RdfId>()) {
                if (param.type.jvmErasure == String::class) {
                    args[param] = resource.value
                } else if (param.type.jvmErasure == Resource::class) {
                    // Injecting raw Jena resource if asked (for legacy compatibility)
                    if (resource is JenaNamedNode) args[param] = resource.node
                    else if (resource is JenaBlankNode) args[param] = resource.node
                }
                continue
            }

            val rdfProp = param.findAnnotation<RdfProperty>()
            if (rdfProp != null) {
                val propUri = rdfProp.uri
                val quads = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(propUri))).toList()
                
                val paramClass = param.type.jvmErasure

                if (paramClass.isSubclassOf(Collection::class)) {
                    val typeArg = param.type.arguments.firstOrNull()?.type?.jvmErasure ?: Any::class
                    val items = quads.map { quad ->
                        mapValue(dataset, quad.`object`, typeArg, param, quad)
                    }
                    if (paramClass.isSubclassOf(Set::class)) {
                        args[param] = items.toSet()
                    } else {
                        args[param] = items.toList()
                    }
                } else {
                    val quad = quads.firstOrNull()
                    if (quad != null) {
                        args[param] = mapValue(dataset, quad.`object`, paramClass, param, quad)
                    } else if (!param.isOptional && !param.type.isMarkedNullable) {
                        throw IllegalArgumentException("Missing required property ${rdfProp.uri} for parameter ${param.name} in class $concreteClass")
                    }
                }
                continue
            }

            val shortcutProp = param.findAnnotation<RdfShortcutProperty>()
            if (shortcutProp != null) {
                val quads = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(shortcutProp.uri))).toList()
                if (quads.isNotEmpty()) {
                    val virtualModel = ModelFactory.createDefaultModel()
                    val virtualSubject = virtualModel.createResource()
                    val virtualNode = when(val o = quads.first().`object`) {
                        is JenaNamedNode -> o.node
                        is JenaLiteral -> o.node
                        is JenaBlankNode -> virtualModel.createResource(org.apache.jena.rdf.model.AnonId(o.value))
                        else -> throw IllegalArgumentException()
                    }
                    virtualModel.add(virtualSubject, virtualModel.createProperty(shortcutProp.shortcutFor), virtualNode)
                    
                    val unionModel = ModelFactory.createUnion((dataset as JenaDatasetCore).model, virtualModel)
                    
                    val virtualQuad = quads.first() // The shortcut quad
                    val paramClass = param.type.jvmErasure
                    args[param] = mapInternal(JenaDatasetCore(unionModel), rdfobjectloader.JenaBlankNode(virtualSubject), setOf(paramClass), virtualQuad)
                    continue
                }
            }

            // Fallback for interfaces/abstract classes mapped by predicate
            val mappedByOneOf = param.findAnnotation<RdfMappedFrom>()
            if (mappedByOneOf != null) {
                for (sub in mappedByOneOf.classes) {
                    val mappedBy = sub.findAnnotation<MappedByPredicate>()
                    if (mappedBy != null) {
                        val matchingQuads = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(mappedBy.uri))).toList()
                        if (matchingQuads.isNotEmpty()) {
                            val quad = matchingQuads.first()
                            args[param] = mapValue(dataset, quad.`object`, sub, param, quad)
                            break
                        }
                    }
                }
            }
        }

        val instance = constructor.callBy(args)
        cache[resource] = instance

        // Inject mutable properties
        for (prop in concreteClass.memberProperties) {
            if (prop !is KMutableProperty<*>) continue
            println("Prop: ${prop.name}, Anns: ${prop.annotations}")
            val originProp = prop.findAnnotation<OriginQuad>()
            if (originProp != null) {
                prop.setter.call(instance, triggeringQuad)
                continue
            }

            val originOfProp = prop.findAnnotation<OriginOfProperty>()
            if (originOfProp != null) {
                val quad = getOriginOfPropertyQuad(dataset, resource, concreteClass, originOfProp.propertyName)
                if (quad != null) {
                    prop.setter.call(instance, StatementParts(quad, subject = false, predicate = false, `object` = true))
                }
                continue
            }

            val rdfProp = prop.findAnnotation<RdfProperty>()
            if (rdfProp != null) {
                val propUri = rdfProp.uri
                val quads = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(propUri))).toList()
                val paramClass = prop.returnType.jvmErasure

                if (paramClass.isSubclassOf(Collection::class)) {
                    val typeArg = prop.returnType.arguments.firstOrNull()?.type?.jvmErasure ?: Any::class
                    val items = quads.map { quad ->
                        mapValue(dataset, quad.`object`, typeArg, null, quad)
                    }
                    if (paramClass.isSubclassOf(Set::class)) {
                        prop.setter.call(instance, items.toSet())
                    } else if (paramClass.isSubclassOf(MutableList::class)) {
                        prop.setter.call(instance, items.toMutableList())
                    } else {
                        prop.setter.call(instance, items.toList())
                    }
                } else {
                    val quad = quads.firstOrNull()
                    if (quad != null) {
                        prop.setter.call(instance, mapValue(dataset, quad.`object`, paramClass, null, quad))
                    }
                }
                continue
            }

            val shortcutProp = prop.findAnnotation<RdfShortcutProperty>()
            if (shortcutProp != null) {
                val quads = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(shortcutProp.uri))).toList()
                println("Quads size for shortcut: ${quads.size}")
                if (quads.isNotEmpty()) {
                    val virtualModel = ModelFactory.createDefaultModel()
                    val virtualSubject = virtualModel.createResource()
                    val virtualNode = when(val o = quads.first().`object`) {
                        is JenaNamedNode -> o.node
                        is JenaLiteral -> o.node
                        is JenaBlankNode -> virtualModel.createResource(org.apache.jena.rdf.model.AnonId(o.value))
                        else -> throw IllegalArgumentException()
                    }
                    virtualModel.add(virtualSubject, virtualModel.createProperty(shortcutProp.shortcutFor), virtualNode)
                    
                    val unionModel = ModelFactory.createUnion((dataset as JenaDatasetCore).model, virtualModel)
                    
                    val virtualQuad = quads.first()
                    val paramClass = prop.returnType.jvmErasure
                    val mapped = mapInternal(JenaDatasetCore(unionModel), rdfobjectloader.JenaBlankNode(virtualSubject), setOf(paramClass), virtualQuad)
                    prop.setter.call(instance, mapped)
                    continue
                }
            }

            // Fallback for mutable properties of abstract interfaces
            val mappedByOneOf = prop.findAnnotation<RdfMappedFrom>()
            if (mappedByOneOf != null) {
                for (sub in mappedByOneOf.classes) {
                    val mappedBy = sub.findAnnotation<MappedByPredicate>()
                    if (mappedBy != null) {
                        val matchingQuads = dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(mappedBy.uri))).toList()
                        if (matchingQuads.isNotEmpty()) {
                            val quad = matchingQuads.first()
                            prop.setter.call(instance, mapValue(dataset, quad.`object`, sub, null, quad))
                            break
                        }
                    }
                }
            }
        }

        return instance
    }

    private fun mapValue(dataset: DatasetCore, value: Term, targetClass: KClass<*>, param: KParameter?, triggeringQuad: Quad? = null): Any {
        return when (targetClass) {
            String::class -> value.value
            Int::class -> value.value.toInt()
            Long::class -> value.value.toLong()
            Boolean::class -> value.value.toBoolean()
            Float::class -> value.value.toFloat()
            Double::class -> value.value.toDouble()
            Resource::class -> {
                when (value) {
                    is JenaNamedNode -> value.node
                    is JenaBlankNode -> value.node
                    else -> throw IllegalArgumentException("Cannot map Literal to Resource")
                }
            }
            RDFNode::class -> {
                when (value) {
                    is JenaNamedNode -> value.node
                    is JenaBlankNode -> value.node
                    is JenaLiteral -> value.node
                    else -> throw IllegalArgumentException("Unsupported Term type")
                }
            }
            else -> {
                // Nested object mapping
                var candidateClasses = setOf(targetClass)
                
                if (targetClass.isAbstract || targetClass.java.isInterface) {
                    val spiDeciders = java.util.ServiceLoader.load(TypeDecider::class.java).toList()
                    for (decider in spiDeciders) {
                        val decided = decider.decide(dataset, value, targetClass)
                        if (decided.isNotEmpty()) {
                            candidateClasses = decided
                            break
                        }
                    }
                }

                mapInternal(dataset, value, candidateClasses, triggeringQuad)
            }
        }
    }

    override fun addDecidableType(rdfType: NamedNode, type: KClass<*>): RdfObjectLoader {
        decidableTypes[rdfType.value] = type
        return this
    }

    override fun bindInterfaceImplementation(interfaze: KClass<*>, implementation: KClass<*>): RdfObjectLoader {
        interfaceBindings[interfaze] = implementation
        return this
    }

    private fun getOriginOfPropertyQuad(
        dataset: DatasetCore,
        resource: Term,
        concreteClass: KClass<*>,
        propertyName: String
    ): Quad? {
        val param = concreteClass.primaryConstructor?.parameters?.find { it.name == propertyName }
        var uri: String? = null
        if (param != null) {
            uri = param.findAnnotation<RdfProperty>()?.uri ?: param.findAnnotation<RdfShortcutProperty>()?.uri
        }
        if (uri == null) {
            val prop = concreteClass.memberProperties.find { it.name == propertyName }
            if (prop != null) {
                uri = prop.findAnnotation<RdfProperty>()?.uri ?: prop.findAnnotation<RdfShortcutProperty>()?.uri
            }
        }
        if (uri != null) {
            return dataset.match(subject = resource, predicate = JenaNamedNode(org.apache.jena.rdf.model.ResourceFactory.createProperty(uri))).firstOrNull()
        }
        return null
    }
}
