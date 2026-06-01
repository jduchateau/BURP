package turtleprov

import rdf.Term
import rdfkt.*
import rdfkt.Quad.Companion.asLiteralTerm
import rdfobjectloader.Point

class RDF12Converter {
    private var nextBlankId = 0

    private fun nextReifierId(): BlankTerm {
        return BlankTerm("reifier_${nextBlankId++}")
    }

    fun toQuads(store: ProvStore): List<Quad> {
        val result = mutableListOf<Quad>()

        fun addNodeAnnotations(mainQuad: Quad, kindProperty: NamedTerm, nodeInfo: NodeInfo) {
            val reifier = nextReifierId()
            result.add(Quad(reifier, RDF.reifies, mainQuad))
            result.add(Quad(reifier, RDF.type, kindProperty))
            nodeInfo.kind?.let { kind ->
                result.add(Quad(reifier, RDEV.TOKEN, NamedTerm(kind.uri)))
            }

            fun addPoint(lineProp: NamedTerm, colProp: NamedTerm, pt: Point?) = pt?.let {
                result.add(Quad(reifier, lineProp, it.line.asLiteralTerm()))
                result.add(Quad(reifier, colProp, it.column.asLiteralTerm()))
            }

            addPoint(RDEV.START_LINE, RDEV.START_COLUMN, nodeInfo.start)
            addPoint(RDEV.END_LINE, RDEV.END_COLUMN, nodeInfo.end)

            addPoint(RDEV.STRING_START_LINE, RDEV.STRING_START_COLUMN, nodeInfo.rdfLiteralStringStart)
            addPoint(RDEV.STRING_END_LINE, RDEV.STRING_END_COLUMN, nodeInfo.rdfLiteralStringEnd)

            nodeInfo.blankNodeId?.let {
                result.add(Quad(reifier, RDEV.BLANK_NODE_ID, it.asLiteralTerm()))
            }
        }

        for (provQuad in store.quads) {
            val quad = provQuad.quad
            result.add(quad)

            provQuad.subjectInfo?.let { addNodeAnnotations(quad, RDEV.SUBJECT, it) }
            provQuad.predicateInfo?.let { addNodeAnnotations(quad, RDEV.PREDICATE, it) }
            provQuad.objectInfo?.let { addNodeAnnotations(quad, RDEV.OBJECT, it) }
        }

        return result
    }

    data class TripleInfo(
        val subjectInfo: NodeInfo?,
        val predicateInfo: NodeInfo?,
        val objectInfo: NodeInfo?
    ) {
        fun toList(): List<NodeInfo?> = listOf(subjectInfo, predicateInfo, objectInfo)
    }

    fun fromAnnotations(targetQuad: rdf.Quad, allQuads: Iterable<rdf.Quad>): TripleInfo {
        val reifiers = allQuads.filter { q ->
            q.predicate.value == RDF.reifies.value &&
                    q.`object` is rdf.Quad &&
                    run {
                        val objQuad = q.`object` as rdf.Quad
                        objQuad.subject.value == targetQuad.subject.value &&
                                objQuad.predicate.value == targetQuad.predicate.value &&
                                objQuad.`object`.value == targetQuad.`object`.value
                    }
        }.map { it.subject }

        var subjInfo: NodeInfo? = null
        var predInfo: NodeInfo? = null
        var objInfo: NodeInfo? = null

        for (reifier in reifiers) {
            val typeQuad = allQuads.find { it.subject.value == reifier.value && it.predicate.value == RDF.type.value }
            val typeUri = typeQuad?.`object`?.value
            val info = extractNodeInfo(reifier, allQuads)
            when (typeUri) {
                RDEV.SUBJECT.value -> subjInfo = info
                RDEV.PREDICATE.value -> predInfo = info
                RDEV.OBJECT.value -> objInfo = info
            }
        }
        return TripleInfo(subjInfo, predInfo, objInfo)
    }

    private fun extractNodeInfo(reifier: Term, allQuads: Iterable<rdf.Quad>): NodeInfo {
        val reifierQuads = allQuads.filter { it.subject.value == reifier.value }

        fun intProp(p: NamedTerm): Int? {
            return reifierQuads.find { it.predicate.value == p.value }?.`object`?.value?.toIntOrNull()
        }

        fun stringProp(p: NamedTerm): String? {
            return reifierQuads.find { it.predicate.value == p.value }?.`object`?.value
        }

        fun point(line: NamedTerm, column: NamedTerm): Point? {
            val lineVal = intProp(line)
            val columnVal = intProp(column)
            return if (lineVal != null && columnVal != null) Point(lineVal, columnVal) else null
        }

        val tokenUri = stringProp(RDEV.TOKEN)

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
