package turtleprov.kotlin

import burp.reporting.StatementPart
import org.apache.jena.datatypes.TypeMapper
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF
import rdf.*
import rdf.Literal

class JenaConverter(private val model: Model = ModelFactory.createDefaultModel()) {
    private val reifieresIds = hashMapOf<Quad, AnonId>()

    fun NamedTerm.toJenaResource() = model.createResource(this.value)
    fun NamedTerm.toJenaProperty() = model.createProperty(this.value)
    fun BlankTerm.toJenaAnonResource() = model.createResource(AnonId(this.value))
    fun Quad.toJenaStatement(): Statement {
        val subj = this.s.toJenaSubject()
        val pred = this.p.toJenaProperty()
        val obj = this.o.toJenaObject()
        return model.createStatement(subj, pred, obj)
    }

    fun Literal.toJenaLiteral() = if (this.type != null) model.createTypedLiteral(
        this.value,
        TypeMapper.getInstance().getSafeTypeByName(this.type.value)
    ) else model.createLiteral(this.value, this.lang)


    fun BlankNodeOrIRI.toJenaSubject(): Resource = when (this) {
        is NamedTerm -> this.toJenaResource()
        is BlankTerm -> this.toJenaAnonResource()
        is Quad -> {
            val id = reifieresIds.getOrPut(this) {
                val id = AnonId()
                model.add(model.createResource(id), RDF.reifies, model.createStatementTerm(this.toJenaStatement()))
                id
            }
            model.createResource(id)
        }
    }

    fun BlankNodeOrIRI.toJenaObject(): RDFNode = when (this) {
        is NamedTerm -> this.toJenaResource()
        is BlankTerm -> this.toJenaAnonResource()
        is Quad -> model.createStatementTerm(this.toJenaStatement())
    }

    fun Term.toJenaObject(): RDFNode = when (this) {
        is BlankNodeOrIRI -> this.toJenaObject()
        is Literal -> this.toJenaLiteral()
    }

    fun ProvStore.toJenaDataset(): Dataset {
        val model = ModelFactory.createDefaultModel()

        // Add prefixes
        for ((prefix, uri) in prefixes) {
            model.setNsPrefix(prefix, uri)
        }

        fun addNodeAnnotations(stmt: StatementTerm, kindProperty: NamedTerm, nodeInfo: NodeInfo) {
            val reifier = model.createResource()
            model.add(reifier, RDF.reifies, stmt)

            reifier.addProperty(RDF.type, kindProperty.toJenaResource())
            reifier.addProperty(RDEV.TOKEN.toJenaProperty(), model.createResource(nodeInfo.kind.uri))

            nodeInfo.start?.let { reifier.addLiteral(RDEV.START_LINE.toJenaProperty(), it.line) }
            nodeInfo.start?.let { reifier.addLiteral(RDEV.START_COLUMN.toJenaProperty(), it.column) }
            nodeInfo.end?.let { reifier.addLiteral(RDEV.END_LINE.toJenaProperty(), it.line) }
            nodeInfo.end?.let { reifier.addLiteral(RDEV.END_COLUMN.toJenaProperty(), it.column) }

            nodeInfo.blankNodeId?.let { reifier.addProperty(RDEV.BLANK_NODE_ID.toJenaProperty(), it) }
        }

        // Add quads as triples and annotate
        for (provQuad in quads) {
            val quad = provQuad.quad
            val subj = quad.s.toJenaSubject()
            val pred = quad.p.toJenaProperty()
            val obj = quad.o.toJenaObject()

            val stmt = model.createStatement(subj, pred, obj)
            model.add(stmt)

            val toBeAnnotated = model.createStatementTerm(stmt)
            provQuad.subjectInfo?.let { addNodeAnnotations(toBeAnnotated, RDEV.SUBJECT, it) }
            provQuad.predicateInfo?.let { addNodeAnnotations(toBeAnnotated, RDEV.PREDICATE, it) }
            provQuad.objectInfo?.let { addNodeAnnotations(toBeAnnotated, RDEV.OBJECT, it) }
        }
        return DatasetFactory.create(model)
    }


    data class TripleInfo(val subjectInfo: NodeInfo?, val predicateInfo: NodeInfo?, val objectInfo: NodeInfo?) {
        fun toList(): List<NodeInfo?> = listOf(subjectInfo, predicateInfo, objectInfo)
        fun get(part: StatementPart) = when (part) {
            StatementPart.Subject -> subjectInfo
            StatementPart.Predicate -> predicateInfo
            StatementPart.Object -> objectInfo
        }
    }

    fun fromAnnotations(stmt: Statement): TripleInfo {
        val model = stmt.model
        val stmtTerm = model.createStatementTerm(stmt)

        // In SPARQL it would be
        // SELECT ?r WHERE {
        //     ?r rdf:reifies ?stmt .
        //     ?r rdf:type ?typeRes .
        //
        // And we would get 3 ?r with the type rdev:subject, rdev:predicate and rdev:object.
        // From there we can extract:
        // SELECT * WHERE {
        //     ?r rdf:type ?type .
        //     ?r rdev:token ?token .
        //     ?r rdev:startLine ?startLine .
        //     ?r rdev:startColumn ?startColumn .
        //     ?r rdev:endLine ?endLine .
        //     ?r rdev:endColumn ?endColumn .
        //     ?r rdev:blankNodeId ?blankNodeId .
        // }
        //

        val reifiers = model.listSubjectsWithProperty(RDF.reifies, stmtTerm).toSet()

        var subjInfo: NodeInfo? = null
        var predInfo: NodeInfo? = null
        var objInfo: NodeInfo? = null

        for (reifier in reifiers) {
            val typeRes = reifier.getPropertyResourceValue(RDF.type)
            val info = extractNodeInfo(reifier)
            when (typeRes?.uri) {
                RDEV.SUBJECT.value -> subjInfo = info
                RDEV.PREDICATE.value -> predInfo = info
                RDEV.OBJECT.value -> objInfo = info
            }
        }
        return TripleInfo(subjInfo, predInfo, objInfo)
    }

    private fun extractNodeInfo(reifier: Resource): NodeInfo {
        val tokenUri = reifier.getProperty(RDEV.TOKEN.toJenaProperty())?.resource?.uri
        val startLine = reifier.getProperty(RDEV.START_LINE.toJenaProperty())?.int
        val startColumn = reifier.getProperty(RDEV.START_COLUMN.toJenaProperty())?.int
        val endLine = reifier.getProperty(RDEV.END_LINE.toJenaProperty())?.int
        val endColumn = reifier.getProperty(RDEV.END_COLUMN.toJenaProperty())?.int
        val blankNodeId = reifier.getProperty(RDEV.BLANK_NODE_ID.toJenaProperty())?.string
        return NodeInfo(
            kind = TurtleNodeKind.valueOf(tokenUri.toString()),
            start = if (startLine != null && startColumn != null) Point(startLine, startColumn) else null,
            end = if (endLine != null && endColumn != null) Point(endLine, endColumn) else null,
            blankNodeId = blankNodeId
        )
    }
}


