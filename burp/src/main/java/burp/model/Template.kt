package burp.model

import burp.model.TemplateReferenceSafety.*
import burp.reporting.LiteralPart
import burp.reporting.Origin
import burp.reporting.PointRange
import burp.util.toIRISafe
import burp.util.toURISafe
import com.google.common.collect.Lists.cartesianProduct
import org.apache.jena.rdf.model.Statement
import turtleprov.Point
import java.util.regex.Pattern

enum class TemplateReferenceSafety { Unsafe, SafeIRI, SafeURI }

class Template(var template: String, var stmt: Statement) : Expression {
    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence { yieldAll(segments) }
    override fun dependencies(): Sequence<PlanNode> = emptySequence()

    var segments: List<Segment> = parseTemplate()

    // If the term map is a template-valued term map,
    // then the generated RDF term is determined by applying
    // the term generation rules to its template value.
    fun values(i: Iteration, safety: TemplateReferenceSafety): List<String> {
        val evaluatedSegments = segments.map { segment ->
            when (segment) {
                is ReferenceSegment -> {
                    val refVals = segment.reference.getStrings(i)
                    val refValsSafe = when (safety) {
                        SafeIRI -> refVals.map { toIRISafe(it) }
                        SafeURI -> refVals.map { toURISafe(it) }
                        Unsafe -> refVals
                    }
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

    sealed class Segment(val offset: Int, var range: PointRange? = null, override var parent: PlanNode?) : PlanNode {
        override fun dependencies() = children()
    }
    class LiteralSegment(val literal: String, offset: Int, parent: Template) : Segment(offset, parent = parent) {
        override fun children() = emptySequence<PlanNode>()
    }
    class ReferenceSegment(var reference: Reference, offset: Int, parent: Template) : Segment(offset, parent = parent) {
        override fun children() = sequenceOf(reference)
    }


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
                    segments.add(LiteralSegment(escapeLiteral, offset, this))
                    offset += literal.length
                }
                val reference = m.group(1)
                val escapeReference = escape(reference)
                // Cannot buildReference here because we don't yet have the parent Iterator referenceFormulation.
                segments.add(ReferenceSegment(RawReference(escapeReference, Origin()), offset + 1, this))
                offset += reference.length
                rest = rest.substring(m.end())
            } else {
                segments.add(LiteralSegment(escape(rest), offset, this))
                offset += rest.length
                rest = ""
            }
        }
        return constructSegmentsRangeAndReference(segments)
    }

    private fun constructSegmentsRangeAndReference(segments: List<Segment>): List<Segment> {
        if (segments.isEmpty()) return segments

        val points = sequence {
            yieldAll(segments.asSequence().map { Point.fromOffset(template, it.offset) })
            yield(Point.fromOffset(template, template.length))
        }

        segments.asSequence().zip(points.zipWithNext()).forEach { (segment, points) ->
            val (startPoint, endPoint) = points
            segment.range = PointRange(startPoint, endPoint)
            (segment as? ReferenceSegment)?.reference?.origin = Origin(this, listOf(LiteralPart(stmt, segment.range!!)))
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