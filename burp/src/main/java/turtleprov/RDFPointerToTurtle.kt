package turtleprov

import org.apache.jena.rdf.model.Statement
import rdf.Quad
import rdfobjectloader.*

fun Quad.jena(): Statement = (this as? JenaQuad)?.stmt ?: throw IllegalArgumentException("Quad is not a JenaQuad")

fun retrieveTurtleLocation(sourceStatements: List<RDFPointer>): List<PointRange> {
    if (sourceStatements.isEmpty()) return emptyList()
    val converter = RDF12Converter()
    val locations = sourceStatements.flatMap { pointer ->
        val quad = pointer.stmt
        val jenaQuad = quad as? JenaQuad ?: throw IllegalArgumentException("Quad is not a JenaQuad")
        val model = jenaQuad.stmt.model
        val allQuads = JenaDatasetCore(model)

        val infos = converter.fromAnnotations(quad, allQuads)
        when (pointer) {
            is StatementParts -> listOfNotNull(
                if (pointer.subject) infos.subjectInfo?.toRange() else null,
                if (pointer.predicate) infos.predicateInfo?.toRange() else null,
                if (pointer.`object`) infos.objectInfo?.toRange() else null
            )

            is LiteralPart -> {
                val info = infos.objectInfo
                if (info != null) {
                    val objectEnd = pointer.objectRange.end
                    val literalStart = info.rdfLiteralStringStart
                    val literalEnd = info.rdfLiteralStringEnd

                    val newStart = literalStart?.plus(pointer.objectRange.start)
                    val newEnd = if (literalStart != null && objectEnd != null) literalStart + objectEnd else literalEnd

                    if (newStart == null || newEnd == null) emptyList() else listOf(PointRange(newStart, newEnd))
                } else {
                    emptyList()
                }
            }

            else -> listOf()
        }
    }
    return locations
}

private fun NodeInfo.toRange(): PointRange? = this.start?.let { PointRange(it, this.end) }