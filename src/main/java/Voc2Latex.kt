/*
 * RER vocabulary to LaTeX Table Converter
 */

import org.apache.jena.query.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import kotlin.io.path.Path

fun main() {
    val model: Model = ModelFactory.createDefaultModel()

    val inputs = listOf("rer.ttl")

    inputs.forEach { fileName ->
        val filePath = Path("src/main/resources/vocabularies").resolve(fileName)
        val absoluteFileUri = filePath.toAbsolutePath().toUri()
        println("Reading $fileName at $absoluteFileUri")
        model.read(absoluteFileUri.toString(), null, "TURTLE")
    }

    val queryString = """
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX rer: <http://w3id.org/rml/report#>
        
        SELECT ?error ?label ?comment ?cause ?solution ?confusion
        WHERE {
            ?error rdfs:subClassOf+ rer:Error .
            OPTIONAL { ?error rdfs:label ?label }
            OPTIONAL { ?error rdfs:comment ?comment }
            OPTIONAL { ?error rer:potentialCause ?cause }
            OPTIONAL { ?error rer:potentialSolution ?solution }
            OPTIONAL { ?error rer:potentialConfusion ?confusion }
        }
        ORDER BY ?label
    """

    val query = QueryFactory.create(queryString)

    println(
        """
        |% LaTeX Table Body generated from RER Vocabulary
        |\begin{tblr}{
        |   colspec={l X X X X},
        |   hlines
        |}
        |Error Class & Description & Potential Solution & Potential Cause & Potential Confusion \\
        |
    """.trimMargin()
    )

    QueryExecutionFactory.create(query, model).use { qexec ->
        val results = qexec.execSelect()
        while (results.hasNext()) {
            val soln = results.nextSolution()

            // Extract and sanitize strings for LaTeX
            val label = soln.getLiteral("label")?.string ?: ""
            val comment = soln.getLiteral("comment")?.string ?: ""
            val solution = soln.getLiteral("solution")?.string ?: ""
            val cause = soln.getLiteral("cause")?.string ?: ""
            val confusion = soln.getLiteral("confusion")?.string ?: ""

            // Formatting the row
            println(
                """${latexEscape(label)} 
                   |    & ${latexEscape(comment)} 
                   |    & ${latexEscape(solution)} 
                   |    & ${latexEscape(cause)} 
                   |    & ${latexEscape(confusion)} \\""".trimMargin()
            )
            println("")
        }
    }

    println("""\end{tblr}""")
}

// Helper to escape special LaTeX characters
fun latexEscape(input: String): String {
    return input
        .replace("&", "\\&")
        .replace("%", "\\%")
        .replace("$", "\\$")
        .replace("#", "\\#")
        .replace("_", "\\_")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("~", "\\textasciitilde{}")
        .replace("^", "\\^{}")
}