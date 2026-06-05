package burp.model.lv


import kotlin.math.max

fun LogicalIteration.toTable(): String {
    return buildTable(
        headers = map.keys.toList(),
        rows = listOf(map)
    )
}

fun Collection<LogicalIteration>.toTable(): String {
    if (isEmpty()) return ""

    val headers = LinkedHashSet<String>()
    for (iteration in this) {
        headers.addAll(iteration.map.keys)
    }

    return buildTable(
        headers = headers.toList(),
        rows = map { it.map }
    )
}

private fun buildTable(headers: List<String>, rows: Collection<Map<String, Any?>>): String {
    if (headers.isEmpty()) return ""

    val widths = computeColumnWidths(headers, rows)
    val line = widths.values.joinToString(separator = "+", prefix = "+", postfix = "+") { w -> "-".repeat(w + 2) }

    val sb = StringBuilder()
    sb.append(line).append("\n")
    appendRow(sb, headers, headers.associateWith { it }, widths)
    sb.append("\n").append(line)

    for (row in rows) {
        sb.append("\n")
        appendRow(sb, headers, row, widths)
    }

    sb.append("\n").append(line)
    return sb.toString()
}

private fun computeColumnWidths(headers: List<String>, rows: Collection<Map<String, Any?>>): Map<String, Int> {
    val widths = LinkedHashMap<String, Int>()
    for (header in headers) {
        var width = header.length
        for (row in rows) {
            width = max(width, valueAsCell(row[header]).length)
        }
        widths[header] = width
    }
    return widths
}

private fun appendRow(
    sb: StringBuilder,
    headers: List<String>,
    row: Map<String, Any?>,
    widths: Map<String, Int>
) {
    sb.append("|")
    for (header in headers) {
        sb.append(" ")
            .append(String.format("%-${widths.getValue(header)}s", valueAsCell(row[header])))
            .append(" |")
    }
}

private fun valueAsCell(value: Any?): String {
    return value.toString()
}