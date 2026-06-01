package burp.reporting

import rdfobjectloader.PointRange
import java.nio.file.Path
import kotlin.math.min

fun extractAndHighlight(
    filePath: Path,
    nodes: List<PointRange>,
    contextLines: Int = 1
): String? {
    val file = filePath.toFile()
    if (!file.exists()) return null

    val allLines = file.readLines()
    if (nodes.isEmpty()) return null

    val highlightsByLine = getMergedHighlights(nodes, allLines)
    if (highlightsByLine.isEmpty()) return null

    val linesToPrint = calculateLinesToPrint(highlightsByLine.keys, allLines.lastIndex, contextLines)

    return buildString {
        var previousLineIndex = -1

        for (lineIndex in linesToPrint) {
            // Add ellipsis if we skipped lines
            if (previousLineIndex != -1 && lineIndex > previousLineIndex + 1) {
                appendLine("...".padStart(6))
            }
            previousLineIndex = lineIndex

            val lineContent = allLines[lineIndex]
            val rangesOnLine = highlightsByLine[lineIndex] ?: emptyList() // Empty if just a context line
            val lineNumberStr = formatLineNumber(lineIndex + 1)

            append(lineNumberStr)
            appendLine(renderLineWithColors(lineContent, rangesOnLine))
        }
    }
}

private fun calculateLinesToPrint(
    highlightedLines: Set<Int>,//0-indexed
    maxLineIndex: Int,
    contextLines: Int
): List<Int> {
    val linesToPrint = mutableSetOf<Int>()
    for (lineIdx in highlightedLines) {
        val start = (lineIdx - contextLines).coerceAtLeast(0)
        val end = (lineIdx + contextLines).coerceAtMost(maxLineIndex)
        for (i in start..end) {
            linesToPrint.add(i)
        }
    }
    return linesToPrint.sorted()
}

private fun formatLineNumber(lineNumber: Int): String = "$lineNumber ".padStart(5) + "| "

/**
 * Splits the line string based on ranges and inserts ANSI codes.
 */
private fun renderLineWithColors(line: String, ranges: List<IntRange>): String {
    if (ranges.isEmpty()) return line

    val sb = StringBuilder()
    var currentIndex = 0

    // Ranges are already sorted and non-overlapping from mergeRanges
    for (range in ranges) {
        // Append text before the highlight
        if (currentIndex < range.first) {
            sb.append(line.substring(currentIndex, range.first))
        }

        // Append highlighted text
        // Ensure we don't go out of bounds if the range extends slightly beyond line length (rare but possible)
        val safeEnd = min(range.last + 1, line.length)
        if (range.first < safeEnd) {
            val textToHighlight = line.substring(range.first, safeEnd)
            sb.append(ansiBoldRed(textToHighlight))
        }

        currentIndex = safeEnd
    }

    // Append the remaining text
    if (currentIndex < line.length) {
        sb.append(line.substring(currentIndex))
    }

    return sb.toString()
}

fun ansiBoldRed(text: String): String = "\u001B[1;31m$text\u001B[0m"
fun ansiRed(text: String): String = "\u001B[31m$text\u001B[0m"