package turtleprov

import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF
import burp.vocabularies.BURP

fun ProvStore.toModel(withAnnotations: Boolean = true): Model {
    val model = ModelFactory.createDefaultModel()

    var nextBlankId = 0
    fun nextReifierId() = model.createResource(AnonId("reifier_${nextBlankId++}"))

    fun addNodeAnnotations(mainQuad: Statement, kindProperty: Resource, nodeInfo: NodeInfo) {
        val reifier = nextReifierId()
        model.add(reifier, RDF.reifies, model.createStatementTerm(mainQuad))
        model.add(reifier, RDF.type, kindProperty)
        nodeInfo.kind?.let { kind ->
            model.add(reifier, BURP.TOKEN, model.createResource(kind.uri))
        }

        fun addPoint(lineProp: Property, colProp: Property, pt: Point?) = pt?.let {
            model.add(reifier, lineProp, model.createTypedLiteral(it.line))
            model.add(reifier, colProp, model.createTypedLiteral(it.column))
        }

        addPoint(BURP.START_LINE, BURP.START_COLUMN, nodeInfo.start)
        addPoint(BURP.END_LINE, BURP.END_COLUMN, nodeInfo.end)

        addPoint(BURP.STRING_START_LINE, BURP.STRING_START_COLUMN, nodeInfo.rdfLiteralStringStart)
        addPoint(BURP.STRING_END_LINE, BURP.STRING_END_COLUMN, nodeInfo.rdfLiteralStringEnd)

        nodeInfo.blankNodeId?.let {
            model.add(reifier, BURP.BLANK_NODE_ID, it)
        }
    }

    for (provQuad in this.triples) {
        val quad = provQuad.statement
        model.add(quad)

        if (withAnnotations) {
            provQuad.subjectInfo?.let { addNodeAnnotations(quad, BURP.SUBJECT, it) }
            provQuad.predicateInfo?.let { addNodeAnnotations(quad, BURP.PREDICATE, it) }
            provQuad.objectInfo?.let { addNodeAnnotations(quad, BURP.OBJECT, it) }
        }
    }

    return model
}

data class TripleInfo(
    val subjectInfo: NodeInfo?,
    val predicateInfo: NodeInfo?,
    val objectInfo: NodeInfo?
) {
    fun toList(): List<NodeInfo?> = listOf(subjectInfo, predicateInfo, objectInfo)
}

fun fromAnnotations(targetTriple: Statement, model: Model): TripleInfo {
    val stmtTerm = model.createStatementTerm(targetTriple)
    val reifiers = model.listSubjectsWithProperty(RDF.reifies, stmtTerm).asSequence()

    var subjInfo: NodeInfo? = null
    var predInfo: NodeInfo? = null
    var objInfo: NodeInfo? = null

    for (reifier in reifiers) {
        val typeRes = reifier.getPropertyResourceValue(RDF.type)
        val info = extractNodeInfo(reifier)
        when (typeRes?.uri) {
            BURP.SUBJECT.uri -> subjInfo = info
            BURP.PREDICATE.uri -> predInfo = info
            BURP.OBJECT.uri -> objInfo = info
        }
    }
    return TripleInfo(subjInfo, predInfo, objInfo)
}

private fun extractNodeInfo(reifier: Resource): NodeInfo {
    fun intProp(p: Property): Int? {
        return reifier.getProperty(p)?.`object`?.asLiteral()?.int
    }

    fun stringProp(p: Property): String? {
        return reifier.getProperty(p)?.`object`?.asLiteral()?.string
    }

    fun uriProp(p: Property): String? {
        return reifier.getProperty(p)?.`object`?.asResource()?.uri
    }

    fun point(line: Property, col: Property): Point? {
        val lineVal = intProp(line)
        val colVal = intProp(col)
        return if (lineVal != null && colVal != null) Point(lineVal, colVal) else null
    }

    val tokenUri = uriProp(BURP.TOKEN)

    return NodeInfo(
        kind = TurtleNodeKind.entries.find { it.uri == tokenUri },
        start = point(BURP.START_LINE, BURP.START_COLUMN),
        end = point(BURP.END_LINE, BURP.END_COLUMN),
        rdfLiteralStringStart = point(BURP.STRING_START_LINE, BURP.STRING_START_COLUMN),
        rdfLiteralStringEnd = point(BURP.STRING_END_LINE, BURP.STRING_END_COLUMN),
        blankNodeId = stringProp(BURP.BLANK_NODE_ID)
    )
}
