package turtleprov

import rdf.Quad

data class Point(val line: Int, val column: Int) : Comparable<Point> {
    constructor(p: org.antlr.v4.kotlinruntime.ast.Point) : this(p.line, p.column)

    override fun compareTo(other: Point): Int {
        val lineCompare = line.compareTo(other.line)
        return if (lineCompare != 0) lineCompare else column.compareTo(other.column)
    }

    operator fun plus(other: Point): Point {
        return Point(line + other.line, column + other.column)
    }

    fun minus(point: Point): Point? {
        val newLine = line - point.line
        val newColumn = column - point.column
        return if (newLine >= 0 && newColumn >= 0) Point(newLine, newColumn) else null
    }

    companion object {
        fun zero() = Point(0, 0)
    }
}

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
        val sLine = start!!.line - 1
        val eLine = end!!.line - 1
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