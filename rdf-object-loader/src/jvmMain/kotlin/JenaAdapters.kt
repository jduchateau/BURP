package rdfobjectloader

import rdfobjectloader.model.*
import org.apache.jena.rdf.model.*

class JenaNamedNode(val node: Resource) : NamedNode {
    override val value: String get() = node.uri
    override fun equals(other: Any?): Boolean = other is NamedNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

class JenaBlankNode(val node: Resource) : BlankNode {
    override val value: String get() = node.id.labelString
    override fun equals(other: Any?): Boolean = other is BlankNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

class JenaLiteral(val node: org.apache.jena.rdf.model.Literal) : rdfobjectloader.model.Literal {
    override val value: String get() = node.lexicalForm
    override val language: String get() = node.language
    override val datatype: NamedNode get() = JenaNamedNode(ResourceFactory.createResource(node.datatypeURI))
    override fun equals(other: Any?): Boolean = other is rdfobjectloader.model.Literal && other.value == value && other.language == language && other.datatype == datatype
    override fun hashCode(): Int = value.hashCode()
}

fun RDFNode.toTerm(): Term = when {
    this.isURIResource -> JenaNamedNode(this.asResource())
    this.isAnon -> JenaBlankNode(this.asResource())
    this.isLiteral -> JenaLiteral(this.asLiteral())
    else -> throw IllegalArgumentException("Unsupported Jena RDFNode type")
}

class JenaQuad(val stmt: Statement) : Quad {
    override val subject: Term get() = stmt.subject.toTerm()
    override val predicate: Term get() = stmt.predicate.toTerm()
    override val `object`: Term get() = stmt.`object`.toTerm()
    override val graph: Term get() = DefaultGraphImpl()
    override fun equals(other: Any?): Boolean {
        if (other !is Quad) return false
        return subject == other.subject && predicate == other.predicate && `object` == other.`object`
    }
    override fun hashCode(): Int = stmt.hashCode()
}

class DefaultGraphImpl : DefaultGraph {
    override fun equals(other: Any?) = other is DefaultGraph
    override fun hashCode() = 0
}

class JenaDatasetCore(val model: Model) : DatasetCore {
    override val size: Int get() = model.size().toInt()

    override fun add(quad: Quad): DatasetCore {
        throw UnsupportedOperationException("Read-only dataset")
    }

    override fun delete(quad: Quad): DatasetCore {
        throw UnsupportedOperationException("Read-only dataset")
    }

    override fun has(quad: Quad): Boolean = false

    override fun match(subject: Term?, predicate: Term?, `object`: Term?, graph: Term?): DatasetCore {
        val s = subject?.let { 
            when (it) {
                is JenaNamedNode -> it.node 
                is JenaBlankNode -> model.createResource(org.apache.jena.rdf.model.AnonId(it.value))
                else -> ResourceFactory.createResource(it.value)
            }
        }
        val p = predicate?.let { ResourceFactory.createProperty(it.value) }
        val o = `object`?.let { 
            when(it) {
                is JenaNamedNode -> ResourceFactory.createResource(it.value)
                is JenaLiteral -> ResourceFactory.createPlainLiteral(it.value)
                is JenaBlankNode -> model.createResource(org.apache.jena.rdf.model.AnonId(it.value))
                else -> null
            }
        }
        val matchedModel = model.listStatements(s, p, o).toModel()
        return JenaDatasetCore(matchedModel)
    }

    override fun iterator(): Iterator<Quad> {
        val stmts = model.listStatements().toList()
        return stmts.map { JenaQuad(it) }.iterator()
    }
}
