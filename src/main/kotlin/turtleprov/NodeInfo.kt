package turtleprov

import org.apache.jena.rdf.model.Statement

/**
 * Represents a character position in a textual grid defined by a line and column.
 *
 * @property line The line number of the character (0-indexed). TODO make zero-indexed otherwise it is horrible to sum and do operations on it
 * @property column The column number of the character (0-indexed).
 *
 * Indexes follow ANTLR conventions.
 */
data class Point(val line: Int, val column: Int) : Comparable<Point> {
    constructor(p: org.antlr.v4.kotlinruntime.ast.Point) : this(p.line-1, p.column)

    val displayLine get() = line + 1
    val displayColumn get() = column + 1

    override fun compareTo(other: Point): Int {
        val lineCompare = line.compareTo(other.line)
        return if (lineCompare != 0) lineCompare else column.compareTo(other.column)
    }

    operator fun plus(other: Point): Point {
        return Point(
            line + other.line ,
            column + other.column
        )
    }

    operator fun minus(point: Point): Point? {
        val newLine = line - point.line
        val newColumn = column - point.column
        return if (newLine >= 0 && newColumn >= 0) Point(newLine, newColumn) else null
    }

    companion object {
        fun zero() = Point(0, 0)

        fun fromOffset(text: String, offset: Int): Point {
            var line = 0
            var column = 0
            for (i in 0 until offset) {
                if (text[i] == '\n') {
                    line++
                    column = 0
                } else {
                    column++
                }
            }
            return Point(line, column)
        }
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
        val sLine = start!!.line
        val eLine = end!!.line
        return sLine..eLine
    }
}

data class ProvTriple(
    val statement: Statement,
    val subjectInfo: NodeInfo?,
    val predicateInfo: NodeInfo?,
    val objectInfo: NodeInfo?,
) {
    constructor(quad: Statement) : this(quad, null, null, null)
}


class ProvStore(
    val triples: MutableSet<ProvTriple> = HashSet(),
    val prefixes: MutableMap<String, String> = mutableMapOf()
)