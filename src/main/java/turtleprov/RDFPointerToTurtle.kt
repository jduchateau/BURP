package turtleprov

import burp.reporting.LiteralPart
import burp.reporting.PointRange
import burp.reporting.RDFGraphPointer
import burp.reporting.StatementParts

fun retrieveTurtleLocation(sourceStatements: List<RDFGraphPointer>): List<PointRange> {
    if (sourceStatements.isEmpty()) return emptyList()
    val locations = sourceStatements.flatMap {
        val infos = fromAnnotations(it.stmt, it.stmt.model)
        when (it) {
            is StatementParts -> listOfNotNull(
                if (it.subject) infos.subjectInfo?.toRange() else null,
                if (it.predicate) infos.predicateInfo?.toRange() else null,
                if (it.`object`) infos.objectInfo?.toRange() else null
            )

            is LiteralPart if infos.objectInfo != null -> {
                val info = infos.objectInfo
                val literalEnd = it.objectRange.end
                val startPt = info.rdfLiteralStringStart
                val endPt = info.rdfLiteralStringEnd
                val newStart = if (startPt != null) startPt + it.objectRange.start else null
                val newEnd =
                    if (startPt != null && literalEnd != null) startPt + literalEnd
                    else endPt

                if (newStart == null || newEnd == null) emptyList() else listOf(PointRange(newStart, newEnd))
            }

            else -> listOf()
        }
    }
    return locations
}


private fun NodeInfo.toRange(): PointRange? = this.start?.let { PointRange(it, this.end) }