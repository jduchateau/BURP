package burp.util

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFLanguages

class BURPConfiguration : CliktCommand(name = "burp") {

    val mappingFile: String by option(
        "-m", "--mappingFile",
        help = "The RML mapping file"
    ).required()

    val outputFile: String? by option(
        "-o", "--outputFile",
        help = "The output file"
    )

    val outputFormat: Lang? by option(
        "-f", "--outputFormat",
        help = "The format of the output file (default: deduced from output file)",
        completionCandidates = CompletionCandidates.Fixed(
            RDFLanguages.strLangNQuads, RDFLanguages.strLangTurtle, RDFLanguages.strLangTriG,
            RDFLanguages.strLangJSONLD, RDFLanguages.strLangRDFXML
        )
    ).convert { RDFLanguages.nameToLang(it) ?: fail("Unknown output format: $it") }

    val baseIRI: String by option(
        "-b", "--baseIRI",
        help = "Used in resolving relative IRIs produced by the RML mapping",
    ).default("http://example.org/")

    val reportFile: String? by option(
        "-r", "--reportFile",
        help = "The report file"
    )

    override fun run() {
        // We just use this command to parse the arguments
    }
}
