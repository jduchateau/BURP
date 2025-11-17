package turtleprov.kotlin

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
            val nodeBN = model.createResource()
            model.add(nodeBN, RDF.reifies, stmt)

            nodeBN.addProperty(RDF.type, kindProperty.toJenaResource())
            nodeBN.addProperty(RDEV.TOKEN.toJenaProperty(), model.createResource(nodeInfo.kind.uri))

            nodeInfo.start?.let { nodeBN.addLiteral(RDEV.START_LINE.toJenaProperty(), it.line) }
            nodeInfo.start?.let { nodeBN.addLiteral(RDEV.START_COLUMN.toJenaProperty(), it.column) }
            nodeInfo.end?.let { nodeBN.addLiteral(RDEV.END_LINE.toJenaProperty(), it.line) }
            nodeInfo.end?.let { nodeBN.addLiteral(RDEV.END_COLUMN.toJenaProperty(), it.column) }

            nodeInfo.blankNodeId?.let { nodeBN.addProperty(RDEV.BLANK_NODE_ID.toJenaProperty(), it) }
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


    data class Triple(val subjectInfo: NodeInfo?, val predicateInfo: NodeInfo?, val objectInfo: NodeInfo?)
    
    fun fromAnnotations(r: Resource) : Triple{
        val model = r.model ?: return Triple(null, null, null)
        val annResources = model.listSubjectsWithProperty(RDF.reifies, r).toList()
        if (annResources.isEmpty()) return Triple(null, null, null)

        var subjInfo: NodeInfo? = null
        var predInfo: NodeInfo? = null
        var objInfo: NodeInfo? = null

        for (ann in annResources) {
            val typeRes = ann.getPropertyResourceValue(RDF.type)
            val info = extractNodeInfo(ann)
            when (typeRes?.uri) {
                RDEV.SUBJECT.value -> subjInfo = info
                RDEV.PREDICATE.value -> predInfo = info
                RDEV.OBJECT.value -> objInfo = info
            }
        }
        return Triple(subjInfo, predInfo, objInfo)
    }

    private fun extractNodeInfo(ann: Resource): NodeInfo {
        val tokenUri = ann.getProperty(RDEV.TOKEN.toJenaProperty())?.resource?.uri
        val startLine = ann.getProperty(RDEV.START_LINE.toJenaProperty())?.int
        val startColumn = ann.getProperty(RDEV.START_COLUMN.toJenaProperty())?.int
        val endLine = ann.getProperty(RDEV.END_LINE.toJenaProperty())?.int
        val endColumn = ann.getProperty(RDEV.END_COLUMN.toJenaProperty())?.int
        val blankNodeId = ann.getProperty(RDEV.BLANK_NODE_ID.toJenaProperty())?.string
        return NodeInfo(
            kind = TurtleNodeKind.valueOf(tokenUri.toString()),
            start = if (startLine != null && startColumn != null) Point(startLine, startColumn) else null,
            end = if (endLine != null && endColumn != null) Point(endLine, endColumn) else null,
            blankNodeId = blankNodeId
        )
    }
}


