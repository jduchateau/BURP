package turtleprov

import rdfkt.Quad
import rdfobjectloader.Point

operator fun Point.Companion.invoke(p: org.antlr.v4.kotlinruntime.ast.Point) = Point(p.line - 1, p.column)

data class NodeInfo(
    val kind: TurtleNodeKind?,
    val start: Point?,
    val end: Point?,
    val rdfLiteralStringStart: Point? = null,
    val rdfLiteralStringEnd: Point? = null,
    val blankNodeId: String? = null // For blank nodes to enable reconstruction
) {
    constructor(
        kind: TurtleNodeKind,
        start: org.antlr.v4.kotlinruntime.ast.Point?,
        end: org.antlr.v4.kotlinruntime.ast.Point?,
        blankNodeId: String? = null
    ) : this(
        kind,
        start?.let { Point(it) },
        end?.let { Point(it) },
        blankNodeId = blankNodeId
    )

    fun lineIndexRange(): IntRange {
        val sLine = start!!.line
        val eLine = end!!.line
        return sLine..eLine
    }
}

data class ProvQuad(
    val quad: Quad,
    val subjectInfo: NodeInfo?,
    val predicateInfo: NodeInfo?,
    val objectInfo: NodeInfo?,
) {
    constructor(quad: Quad) : this(quad, null, null, null)
}

class ProvStore(
    val quads: MutableSet<ProvQuad> = HashSet(),
    val prefixes: MutableMap<String, String> = mutableMapOf()
)