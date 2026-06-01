package burp.reporting

import rdfobjectloader.PointRange
import kotlin.math.max

/**
 * Converts a list of Nodes (which may span multiple lines) into a map of
 * LineIndex -> List<ColumnRange>. Overlapping ranges are merged.
 *
 * @return a map from line indexes (0 indexed) to a list of column ranges (also 0-indexed).
 */
internal fun getMergedHighlights(nodes: List<PointRange>, lines: List<String>): Map<Int, List<IntRange>> {
    val rawMap = mutableMapOf<Int, MutableList<IntRange>>()

    // 1. Flatten Nodes into raw line ranges
    nodes.filter { it.end != null }.forEach { node ->
        val startLine = node.start.line.coerceAtLeast(0)
        val end = node.end
        val endLine = end!!.line.coerceAtMost(lines.lastIndex)

        for (lineIdx in startLine..endLine) {
            val lineLen = lines[lineIdx].length

            val startCol = if (lineIdx == startLine) node.start.column else 0
            val endCol = if (lineIdx == endLine) end.column else lineLen // Go to end of line if multi-line

            // Ensure we don't go out of bounds
            val safeStart = startCol.coerceIn(0, lineLen)
            val safeEnd = endCol.coerceIn(0, lineLen)

            // Add only valid ranges
            if (safeStart <= safeEnd) {
                rawMap.computeIfAbsent(lineIdx) { mutableListOf() }.add(safeStart..safeEnd)
            }
        }
    }

    // 2. Merge overlapping ranges for each line
    return rawMap.mapValues { (_, ranges) -> mergeRanges(ranges) }
}

/**
 * Merges a list of overlapping or adjacent ranges.
 * e.g. [0..5, 4..8, 10..12] -> [0..8, 10..12]
 */
internal fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
    if (ranges.isEmpty()) return emptyList()

    val sorted = ranges.sortedBy { it.first }
    val merged = mutableListOf<IntRange>()

    var current = sorted[0]

    for (i in 1 until sorted.size) {
        val next = sorted[i]
        // If next overlaps or is adjacent to current (next.start <= current.end + 1)
        // We use +1 to merge "adjacent" chars (e.g. 0..1 and 2..3 become 0..3)
        // because visually they should be one block.
        if (next.first <= current.last + 1) {
            current = current.first..max(current.last, next.last)
        } else {
            merged.add(current)
            current = next
        }
    }
    merged.add(current)
    return merged
}