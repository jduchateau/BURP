package burp.reporting

import burp.Main
import burp.vocabularies.RER
import org.apache.jena.ontology.OntClass
import java.nio.file.Path


// TODO handle indentation with StringUtils.leftPad("text", tab_length * 2)
fun generateTextReport(report: RmlEngineReport): String {
    val sb = StringBuilder()

    fun printTracingInfo(sb: StringBuilder, issue: Report) {
        issue.origin?.let { origin ->
            sb.appendLine("In mapping".prependIndent(4))
            val file = Path.of(Main.conf.mappingFile)
            val locations = origin.locations()

            // Print file:line:col-line:col
            locations.sortedBy { it.start }
                .map { location -> sb.appendLine(fileLocationString(file, location).prependIndent(6)) }
        }
    }

    fun printIssues(sb: StringBuilder, issues: List<Report>, header: String) {
        if (issues.isNotEmpty()) {
            sb.appendLine("$header:")
            issues.forEachIndexed { index, issue ->
                sb.appendLine(issue.message.prependIndent(2))
                printTracingInfo(sb, issue)
                if (issue is RmlError) {
                    sb.appendErrorTypeHelp(issue.errorType)

                    // This is just noise
                    // sb.appendLine()
                    // val superClasses = issue.errorType.listSuperClasses().toList()
                    // superClasses.remove(RDFS.Resource)
                    // if (superClasses.isNotEmpty())
                    //     for (superClass in superClasses.asReversed())
                    //         sb.appendErrorTypeHelp(superClass)

                    if (issue.exception != null) {
                        sb.appendLine("Exception: ${issue.exception}".prependIndent(4))
                        sb.appendLine("StackTrace: ${issue.exception.stackTraceToString()}".prependIndent(4))
                    }
                }
                // Add empty line between issues for better readability
                if (index < issues.size - 1) {
                    sb.appendLine()
                }
            }
            sb.appendLine() // Add an extra line after a section
        }
    }

    // Replace the original error and warning blocks with:
    printIssues(sb, report.errors, "Errors")
    printIssues(sb, report.warnings, "Warnings")


    sb.append("Statistics:\n")
    sb.append("  - Generated statements: ${report.statistics?.generatedStatements ?: "?"}\n")
    sb.append("  - Number of triples maps: ${report.executionPlan.size}\n")


    return sb.toString()
}


private fun StringBuilder.appendErrorTypeHelp(issueType: OntClass) {
    appendLine("is a ${issueType.getLabel(null)} ($issueType)".prependIndent(4))
    appendLine("${issueType.getComment(null)}".prependIndent("| ").prependIndent(6))
    val potentialSolution = issueType.getPropertyValue(RER.potentialSolution)
    if (potentialSolution != null)
        appendLine("| Try to: $potentialSolution".prependIndent(6))
}

private fun String.prependIndent(indent: Int): String = prependIndent(" ".repeat(indent))


