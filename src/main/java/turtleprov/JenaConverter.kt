package turtleprov

import burp.reporting.StatementPart
import org.apache.jena.datatypes.TypeMapper
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF
import rdf.*
import rdf.Literal
import kotlin.collections.iterator

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
        this.value, TypeMapper.getInstance().getSafeTypeByName(this.type.value)
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
            reifier.addProperty(RDEV.TOKEN.toJenaProperty(), model.createResource(nodeInfo.kind?.uri))

            fun addPoint(lineProp: NamedTerm, colProp: NamedTerm, pt: Point?) = pt?.let {
                reifier.addLiteral(lineProp.toJenaProperty(), it.line)
                reifier.addLiteral(colProp.toJenaProperty(), it.column)
            }

            addPoint(RDEV.START_LINE, RDEV.START_COLUMN, nodeInfo.start)
            addPoint(RDEV.END_LINE, RDEV.END_COLUMN, nodeInfo.end)

            addPoint(RDEV.STRING_START_LINE, RDEV.STRING_START_COLUMN, nodeInfo.rdfLiteralStringStart)
            addPoint(RDEV.STRING_END_LINE, RDEV.STRING_END_COLUMN, nodeInfo.rdfLiteralStringEnd)

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
        fun intProp(p: NamedTerm) = reifier.getProperty(p.toJenaProperty())?.int
        fun stringProp(p: NamedTerm) = reifier.getProperty(p.toJenaProperty())?.string
        fun uriProp(p: NamedTerm) = reifier.getProperty(p.toJenaProperty())?.resource?.uri
        fun point(line: NamedTerm, column: NamedTerm): Point? {
            val lineVal = intProp(line)
            val columnVal = intProp(column)
            return if (lineVal != null && columnVal != null) Point(lineVal, columnVal) else null
        }

        val tokenUri = uriProp(RDEV.TOKEN)

        return NodeInfo(
            kind = TurtleNodeKind.entries.find { it.uri == tokenUri },

            start = point(RDEV.START_LINE, RDEV.START_COLUMN),
            end = point(RDEV.END_LINE, RDEV.END_COLUMN),

            rdfLiteralStringStart = point(RDEV.STRING_START_LINE, RDEV.STRING_START_COLUMN),
            rdfLiteralStringEnd = point(RDEV.STRING_END_LINE, RDEV.STRING_END_COLUMN),

            blankNodeId = stringProp(RDEV.BLANK_NODE_ID)
        )
    }
}


