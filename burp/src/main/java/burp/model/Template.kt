package burp.model

import burp.model.TemplateReferenceSafety.*
import burp.util.toIRISafe
import burp.util.toURISafe
import burp.vocabularies.Rml
import com.google.common.collect.Lists.cartesianProduct
import rdfobjectloader.LiteralPart
import rdfobjectloader.Point
import rdfobjectloader.PointRange
import rdfobjectloader.StatementParts
import rdfobjectloader.annotations.MappedByPredicate
import rdfobjectloader.annotations.OriginOfProperty
import rdfobjectloader.annotations.RdfLiteral
import java.util.regex.Pattern

enum class TemplateReferenceSafety { Unsafe, SafeIRI, SafeURI }

@MappedByPredicate(Rml.template)
class Template(
    @RdfLiteral
    var template: String,
    @OriginOfProperty("template")
    var templateOrigin: StatementParts
) : Expression {
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

    sealed class Segment(var range: PointRange, override var parent: PlanNode?) : PlanNode {
        override fun dependencies() = children()
    }

    class LiteralSegment(val literal: String, range: PointRange, parent: Template) : Segment(range, parent = parent) {
        override fun children() = emptySequence<PlanNode>()
    }

    class ReferenceSegment(var reference: Reference, range: PointRange, parent: Template) :
        Segment(range, parent = parent) {
        override fun children() = sequenceOf(reference)
    }

    enum class SegmentKind { Literal, Reference }

    private fun parseTemplate(): List<Segment> {
        var rest = template
        var offset = 0
        val segments = mutableListOf<Triple<SegmentKind, String, Int>>()
        while (rest.isNotEmpty()) {
            val m = bracesPattern.matcher(rest)
            if (m.find()) {
                if (m.start() > 0) {
                    val literal = rest.take(m.start(1) - 1)
                    val escapeLiteral = escape(literal)
                    segments.add(Triple(SegmentKind.Literal, escapeLiteral, offset))
                    offset += literal.length
                }
                val reference = m.group(1)
                val escapeReference = escape(reference)
                // Cannot buildReference here because we don't yet have the parent Iterator referenceFormulation.
                segments.add(Triple(SegmentKind.Reference, escapeReference, offset + 1))
                offset += reference.length
                rest = rest.substring(m.end())
            } else {
                segments.add(Triple(SegmentKind.Literal, escape(rest), offset))
                offset += rest.length
                rest = ""
            }
        }
        return constructSegmentsRangeAndReference(segments)
    }

    private fun constructSegmentsRangeAndReference(segments: List<Triple<SegmentKind, String, Int>>): List<Segment> {
        if (segments.isEmpty()) return emptyList()

        val points = sequence {
            yieldAll(segments.asSequence().map { Point.fromOffset(template, it.third) })
            yield(Point.fromOffset(template, template.length))
        }

        val constructedSegments = segments.asSequence().zip(points.zipWithNext()).map { (segment, points) ->
            val (startPoint, endPoint) = points
            val range = PointRange(startPoint, endPoint)

            when (segment.first) {
                SegmentKind.Literal -> LiteralSegment(segment.second, range, this)
                SegmentKind.Reference -> ReferenceSegment(
                    RawReference(segment.second, LiteralPart(templateOrigin.stmt, range)),
                    range,
                    this
                )
            }
        }.toList()

        return constructedSegments
    }

    private fun escape(s: String): String {
        return s.replace("""\\{""", "{").replace("""\\}""", "}")
    }

    companion object {
        private val bracesPattern: Pattern = Pattern.compile("""(?<!\\)\{(.+?)(?<!\\)}""")
    }
}