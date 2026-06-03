package rdfkt

import org.apache.jena.rdf.model.*
import rdf.BlankNode
import rdf.DatasetCore
import rdf.NamedNode

class JenaNamedNode(val node: Resource) : NamedNode {
    override val value: String get() = node.uri
    override fun equals(other: Any?): Boolean = other is NamedNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value
}

class JenaBlankNode(val node: Resource) : BlankNode {
    override val value: String get() = node.id.labelString
    override fun equals(other: Any?): Boolean = other is BlankNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "_:$value"
}

class JenaLiteral(val node: org.apache.jena.rdf.model.Literal) : rdf.Literal {
    override val value: String get() = node.lexicalForm
    override val language: String get() = node.language
    override val datatype: NamedNode get() = JenaNamedNode(ResourceFactory.createResource(node.datatypeURI))
    override fun equals(other: Any?): Boolean =
        other is rdf.Literal && other.value == value && other.language == language && other.datatype == datatype

    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "\"$value\""
}

fun RDFNode.toTerm(): rdf.Term = when {
    this.isURIResource -> JenaNamedNode(this.asResource())
    this.isAnon -> JenaBlankNode(this.asResource())
    this.isLiteral -> JenaLiteral(this.asLiteral())
    this.isStatementTerm -> JenaQuad(this.asStatementTerm().statement)
    else -> throw IllegalArgumentException("Unsupported Jena RDFNode type: $this")
}

class JenaQuad(val stmt: Statement) : rdf.Quad, rdf.Term {
    override val termType: String get() = "Quad"
    override val value: String get() = stmt.toString()

    override val subject: rdf.Term get() = stmt.subject.toTerm()
    override val predicate: rdf.Term get() = stmt.predicate.toTerm()
    override val `object`: rdf.Term get() = stmt.`object`.toTerm()
    override val graph: rdf.Term get() = DefaultGraphImpl()
    override fun equals(other: Any?): Boolean {
        if (other !is rdf.Quad) return false
        return subject == other.subject && predicate == other.predicate && `object` == other.`object`
    }

    override fun hashCode(): Int = stmt.hashCode()
    override fun toString(): String = "$subject $predicate $`object` ."
}

class DefaultGraphImpl : rdf.DefaultGraph {
    override fun equals(other: Any?) = other is rdf.DefaultGraph
    override fun hashCode() = 0
    override fun toString(): String = ""
}

class JenaDataset(val model: Model) : DatasetCore {
    override val size: Int get() = model.size().toInt()

    override fun add(quad: rdf.Quad): DatasetCore {
        throw UnsupportedOperationException("Read-only dataset")
    }

    override fun delete(quad: rdf.Quad): DatasetCore {
        throw UnsupportedOperationException("Read-only dataset")
    }

    override fun has(quad: rdf.Quad): Boolean = false

    override fun match(
        subject: rdf.Term?,
        predicate: rdf.Term?,
        `object`: rdf.Term?,
        graph: rdf.Term?
    ): DatasetCore {
        val s = subject?.let {
            when (it) {
                is NamedNode -> ResourceFactory.createResource(it.value)
                is BlankNode -> model.createResource(AnonId(it.value))
                else -> ResourceFactory.createResource(it.value)
            }
        }
        val p = predicate?.let { ResourceFactory.createProperty(it.value) }
        val o: RDFNode? = `object`?.let {
            when (it) {
                is NamedNode -> ResourceFactory.createResource(it.value)
                is Literal -> ResourceFactory.createPlainLiteral(it.value)
                is BlankNode -> model.createResource(AnonId(it.value))
                else -> throw IllegalArgumentException("Unsupported object type: ${it::class.qualifiedName}")
            }
        }
        val matchedModel = model.listStatements(s, p, o).toModel()
        return JenaDataset(matchedModel)
    }

    override fun iterator(): Iterator<rdf.Quad> {
        val stmts = model.listStatements().toList()
        return stmts.map { JenaQuad(it) }.iterator()
    }
}
