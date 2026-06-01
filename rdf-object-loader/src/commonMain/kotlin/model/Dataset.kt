package rdfobjectloader.model

interface Quad {
    val subject: Term
    val predicate: Term
    val `object`: Term
    val graph: Term
    override fun equals(other: Any?): Boolean
}

interface DatasetCore : Iterable<Quad> {
    val size: Int
    fun add(quad: Quad): DatasetCore
    fun delete(quad: Quad): DatasetCore
    fun has(quad: Quad): Boolean
    fun match(subject: Term? = null, predicate: Term? = null, `object`: Term? = null, graph: Term? = null): DatasetCore
    override operator fun iterator(): Iterator<Quad>
}

interface DataFactory {
    fun namedNode(value: String): NamedNode
    fun blankNode(value: String? = null): BlankNode
    fun literal(value: String, languageOrDatatype: Any? = null): Literal
    fun variable(value: String): Variable
    fun defaultGraph(): DefaultGraph
    fun quad(subject: Term, predicate: Term, `object`: Term, graph: Term? = null): Quad
}
