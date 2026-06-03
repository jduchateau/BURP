package rdfkt

class N3NamedNode(val node: dynamic) : rdf.NamedNode {
    override val value: String get() = node.value as String
    override fun equals(other: Any?): Boolean = other is rdf.NamedNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value
}

class N3BlankNode(val node: dynamic) : rdf.BlankNode {
    override val value: String get() = node.value as String
    override fun equals(other: Any?): Boolean = other is rdf.BlankNode && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "_:$value"
}

class N3Literal(val node: dynamic) : rdf.Literal {
    override val value: String get() = node.value as String
    override val language: String get() = (node.language as String?) ?: ""
    override val datatype: rdf.NamedNode get() {
        val dt = node.datatype
        return if (dt != null) N3NamedNode(dt) else N3NamedNode(js("{ value: 'http://www.w3.org/2001/XMLSchema#string' }"))
    }
    override fun equals(other: Any?): Boolean = other is rdf.Literal && other.value == value && other.language == language && other.datatype == datatype
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "\"$value\""
}

class N3DefaultGraph(val node: dynamic) : rdf.DefaultGraph {
    override fun equals(other: Any?) = other is rdf.DefaultGraph
    override fun hashCode() = 0
    override fun toString(): String = ""
}

fun mapN3Term(t: dynamic): rdf.Term = when (t.termType as String) {
    "NamedNode" -> N3NamedNode(t)
    "BlankNode" -> N3BlankNode(t)
    "Literal" -> N3Literal(t)
    "DefaultGraph" -> N3DefaultGraph(t)
    "Quad" -> N3Quad(t)
    else -> throw IllegalArgumentException("Unknown N3 term type: ${t.termType}")
}

class N3Quad(val quad: dynamic) : rdf.Quad, rdf.Term {
    override val termType: String get() = "Quad"
    override val value: String get() = quad.toString()

    override val subject: rdf.Term get() = mapN3Term(quad.subject)
    override val predicate: rdf.Term get() = mapN3Term(quad.predicate)
    override val `object`: rdf.Term get() = mapN3Term(quad.`object`)
    override val graph: rdf.Term get() = mapN3Term(quad.graph)

    override fun equals(other: Any?): Boolean {
        if (other !is rdf.Quad) return false
        return subject == other.subject && predicate == other.predicate && `object` == other.`object` && graph == other.graph
    }
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "$subject $predicate $`object` ."
}

class N3Dataset(val store: dynamic) : rdf.DatasetCore {
    override val size: Int get() = store.size as Int

    override fun add(quad: rdf.Quad): rdf.DatasetCore {
        throw UnsupportedOperationException("Read-only dataset")
    }

    override fun delete(quad: rdf.Quad): rdf.DatasetCore {
        throw UnsupportedOperationException("Read-only dataset")
    }

    override fun has(quad: rdf.Quad): Boolean = false

    override fun match(subject: rdf.Term?, predicate: rdf.Term?, `object`: rdf.Term?, graph: rdf.Term?): rdf.DatasetCore {
        val n3 = js("require('n3')")
        val factory = n3.DataFactory
        
        val s = subject?.let {
            when (it) {
                is N3NamedNode -> it.node
                is N3BlankNode -> it.node
                is rdf.NamedNode -> factory.namedNode(it.value)
                is rdf.BlankNode -> factory.blankNode(it.value)
                else -> factory.namedNode(it.value)
            }
        }
        val p = predicate?.let {
            when (it) {
                is N3NamedNode -> it.node
                is rdf.NamedNode -> factory.namedNode(it.value)
                else -> factory.namedNode(it.value)
            }
        }
        val o = `object`?.let {
            when (it) {
                is N3NamedNode -> it.node
                is N3BlankNode -> it.node
                is N3Literal -> it.node
                is rdf.NamedNode -> factory.namedNode(it.value)
                is rdf.BlankNode -> factory.blankNode(it.value)
                is rdf.Literal -> {
                    if (it.language.isNotEmpty()) {
                        factory.literal(it.value, it.language)
                    } else if (it.datatype.value.isNotEmpty()) {
                        factory.literal(it.value, factory.namedNode(it.datatype.value))
                    } else {
                        factory.literal(it.value)
                    }
                }
                else -> factory.literal(it.value)
            }
        }
        val g = graph?.let {
            when (it) {
                is N3NamedNode -> it.node
                is N3BlankNode -> it.node
                is N3DefaultGraph -> it.node
                is rdf.NamedNode -> factory.namedNode(it.value)
                is rdf.BlankNode -> factory.blankNode(it.value)
                is rdf.DefaultGraph -> factory.defaultGraph()
                else -> factory.defaultGraph()
            }
        }
        
        val matchedQuads = store.getQuads(s, p, o, g)
        val newStore = js("new n3.Store()")
        newStore.addQuads(matchedQuads)
        return N3Dataset(newStore)
    }

    override fun iterator(): Iterator<rdf.Quad> {
        val quads = store.getQuads(null, null, null, null)
        val len = quads.length as Int
        val list = mutableListOf<rdf.Quad>()
        for (i in 0 until len) {
            list.add(N3Quad(quads[i]))
        }
        return list.iterator()
    }
}
