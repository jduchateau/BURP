package burp.model

sealed interface Term

sealed interface BlankNodeOrIRI : Term

data class BlankNodeTerm(val id: String) : BlankNodeOrIRI {
    override fun toString() = "_:$id"
}

data class IRITerm(val uri: String) : BlankNodeOrIRI {
    override fun toString() = "<$uri>"
}

data class LiteralTerm(
    val value: String, val datatype: IRITerm? = null, val language: String? = null
) : Term {
    override fun toString(): String = when {
        language != null -> "\"$value\"@$language"
        datatype != null -> "\"$value\"^^${datatype.uri}"
        else -> "\"$value\""
    }
}

typealias GraphId = IRITerm?

// -----------------------------------------------------
// Container and Collection Structures for RML-CC
// -----------------------------------------------------

sealed class CollectionOrContainerTerm(open val idGenerated: Boolean) : BlankNodeOrIRI {
    abstract val elements: MutableList<Term>
    abstract var id: BlankNodeOrIRI
}

data class RdfListTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

data class RdfBagTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

data class RdfSeqTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

data class RdfAltTerm(
    override val elements: MutableList<Term>, override var id: BlankNodeOrIRI, override val idGenerated: Boolean
) : CollectionOrContainerTerm(idGenerated) {}

// -----------------------------------------------------
// Statements and partial statements
// -----------------------------------------------------

data class RdfPredicateObject(
    val predicate: IRITerm, val `object`: Term, val graph: GraphId = null
)

sealed interface RdfStatementLike

data class RdfStatement(
    var subject: BlankNodeOrIRI, var predicate: IRITerm, var `object`: Term, var graph: GraphId = null
) : RdfStatementLike

data class RdfStatementSubjectGraph(
    var subject: BlankNodeOrIRI, var graph: GraphId = null
) : RdfStatementLike