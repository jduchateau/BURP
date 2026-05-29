package turtleprov

import burp.reporting.LiteralPart
import burp.reporting.PointRange
import burp.reporting.RDFGraphPointer
import burp.reporting.StatementParts

fun retrieveTurtleLocation(sourceStatements: List<RDFGraphPointer>): List<PointRange> {
    val converter = JenaConverter()
    if (sourceStatements.isEmpty()) return emptyList()
    val locations = sourceStatements.flatMap {
        val infos = converter.fromAnnotations(it.stmt)
        when (it) {
            is StatementParts -> listOfNotNull(
                if (it.subject) infos.subjectInfo?.toRange() else null,
                if (it.predicate) infos.predicateInfo?.toRange() else null,
                if (it.`object`) infos.objectInfo?.toRange() else null
            )

            is LiteralPart if infos.objectInfo != null -> {
                val info = infos.objectInfo
                val objectEnd = it.objectRange.end
                val literalStart = info.rdfLiteralStringStart
                val literalEnd = info.rdfLiteralStringEnd

                val newStart = literalStart?.plus(it.objectRange.start)
                val newEnd = if (literalStart != null && objectEnd != null) literalStart + objectEnd else literalEnd

                if (newStart == null || newEnd == null) emptyList() else listOf(PointRange(newStart, newEnd))
            }

            else -> listOf()
        }
    }
    return locations
}


private fun NodeInfo.toRange(): PointRange? = this.start?.let { PointRange(it, this.end) }