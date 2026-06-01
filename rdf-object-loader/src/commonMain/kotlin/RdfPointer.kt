package rdfobjectloader

import rdfobjectloader.model.Literal
import rdfobjectloader.model.Quad

sealed interface RDFPointer {
    val stmt: Quad
}

enum class StatementPart {
    Subject, Predicate, Object
}

data class StatementParts(
    override val stmt: Quad, val subject: Boolean, val predicate: Boolean, val `object`: Boolean
) : RDFPointer {
    companion object {
        fun from(stmt: Quad, vararg parts: StatementPart): StatementParts {
            return StatementParts(
                stmt, StatementPart.Subject in parts, StatementPart.Predicate in parts, StatementPart.Object in parts
            )
        }

        fun fromPredicateObject(stmt: Quad): StatementParts = StatementParts(
            stmt, subject = false, predicate = true, `object` = true
        )
    }
}

data class LiteralPart(override val stmt: Quad, val objectRange: PointRange) : RDFPointer {
    init {
        if (stmt.`object` !is Literal) throw IllegalArgumentException("Statement object is not a literal: $stmt")
    }
}

data class PointRange(val start: Point, val end: Point? = null) {
    operator fun plus(other: PointRange): PointRange {
        return PointRange(
            start = start + other.start, end = (end ?: Point.zero()) + (other.end ?: Point.zero())
        )
    }
}

/**
 * Represents a character position in a textual grid defined by a line and column.
 *
 * @property line The line number of the character (0-indexed).
 * @property column The column number of the character (0-indexed).
 *
 */
data class Point(val line: Int, val column: Int) : Comparable<Point> {
    val displayLine get() = line + 1
    val displayColumn get() = column + 1

    override fun compareTo(other: Point): Int {
        val lineCompare = line.compareTo(other.line)
        return if (lineCompare != 0) lineCompare else column.compareTo(other.column)
    }

    operator fun plus(other: Point): Point {
        return Point(
            line + other.line,
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