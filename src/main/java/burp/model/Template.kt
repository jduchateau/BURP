package burp.model

import burp.reporting.LiteralPart
import burp.reporting.Origin
import burp.reporting.PointRange
import burp.util.Util
import com.google.common.collect.Lists.cartesianProduct
import org.apache.jena.rdf.model.Statement
import turtleprov.Point
import java.util.regex.Pattern

class Template(var template: String, var stmt: Statement) : Expression() {

    // If the term map is a template-valued term map,
    // then the generated RDF term is determined by applying
    // the term generation rules to its template value.
    fun values(i: Iteration): List<String> {
        return values(i, false)
    }

    fun values(i: Iteration, safe: Boolean): List<String> {
        val segments = parseTemplate()
        val evaluatedSegments = segments.map { segment ->
            when (segment) {
                is ReferenceSegment -> {
                    val origin = Origin(this, listOf(LiteralPart(stmt, segment.range!!)))
                    val refVals = i.getStringsFor(segment.rawInside, origin)
                    val refValsSafe = if (safe) refVals.map { Util.toIRISafe(it) } else refVals
                    refValsSafe
                }

                is LiteralSegment -> {
                    listOf(segment.literal)
                }
            }
        }
        val product = cartesianProduct(evaluatedSegments).map { it.joinToString("") }
        return product
    }

    private sealed class Segment(val offset: Int, var range: PointRange? = null)
    private class LiteralSegment(val literal: String, offset: Int) : Segment(offset)
    private class ReferenceSegment(val rawInside: String, offset: Int) : Segment(offset)

    private fun parseTemplate(): List<Segment> {
        var rest = template
        var offset = 0
        val segments = mutableListOf<Segment>()
        while (rest.isNotEmpty()) {
            val m = bracesPattern.matcher(rest)
            if (m.find()) {
                if (m.start() > 0) {
                    val literal = rest.take(m.start(1) - 1)
                    val escapeLiteral = escape(literal)
                    segments.add(LiteralSegment(escapeLiteral, offset))
                    offset += literal.length
                }
                val reference = m.group(1)
                val escapeReference = escape(reference)
                segments.add(ReferenceSegment(escapeReference, offset+1))
                offset += reference.length
                rest = rest.substring(m.end())
            } else {
                segments.add(LiteralSegment(escape(rest), offset))
                offset += rest.length
                rest = ""
            }
        }
        return enrichSegmentWithPoint(segments)
    }

    private fun enrichSegmentWithPoint(segments: List<Segment>): List<Segment> {
        if (segments.isEmpty()) return segments

        val points = sequence {
            yieldAll(segments.asSequence().map { Point.fromOffset(template, it.offset) })
            yield(Point.fromOffset(template, template.length))
        }

        segments.asSequence().zip(points.zipWithNext()).forEach { (segment, points) ->
            val (startPoint, endPoint) = points
            segment.range = PointRange(startPoint, endPoint)
        }
        return segments
    }

    private fun escape(s: String): String {
        return s.replace("""\\{""", "{").replace("""\\}""", "}")
    }

    companion object {
        private val bracesPattern: Pattern = Pattern.compile("""(?<!\\)\{(.+?)(?<!\\)}""")
    }
}