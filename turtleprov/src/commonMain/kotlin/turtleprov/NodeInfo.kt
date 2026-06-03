@file:OptIn(ExperimentalJsExport::class)

package turtleprov

import rdfkt.Quad
import rdfobjectloader.Point
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

operator fun Point.Companion.invoke(p: org.antlr.v4.kotlinruntime.ast.Point) = Point(p.line - 1, p.column)

@JsExport
data class NodeInfo(
    val kind: TurtleNodeKind?,
    val start: Point?,
    val end: Point?,
    val rdfLiteralStringStart: Point? = null,
    val rdfLiteralStringEnd: Point? = null,
    val blankNodeId: String? = null // For blank nodes to enable reconstruction
) {
    @JsName("create")
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

@JsExport
data class ProvQuad(
    val quad: Quad,
    val subjectInfo: NodeInfo?,
    val predicateInfo: NodeInfo?,
    val objectInfo: NodeInfo?,
) {
    @JsName("fromQuad")
    constructor(quad: Quad) : this(quad, null, null, null)
}

@JsExport
class ProvStore(
    val quads: MutableSet<ProvQuad> = HashSet(),
    val prefixes: MutableMap<String, String> = mutableMapOf()
)