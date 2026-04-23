package burp

import burp.model.*
import burp.parse.Parse
import burp.parse.PlanWiring
import burp.reporting.*
import burp.util.BURPConfiguration
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.github.ajalt.clikt.core.main
import org.apache.jena.datatypes.BaseDatatype
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFLanguages.pathnameToLang
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val cwd = Paths.get("").toAbsolutePath()
        val exit = doMain(args, cwd)
        println("System exiting with code: $exit")
        exitProcess(exit)
    }

    fun doMain(args: Array<String>, currentWorkingDirectory: Path?): Int {
        val conf = BURPConfiguration()
        conf.main(args)

        return doMain(
            conf.mappingFile,
            conf.outputFile,
            conf.outputFormat,
            conf.reportFile,
            conf.baseIRI,
            currentWorkingDirectory
        )
    }

    // Hack to quickly get the config from anywhere
    lateinit var report: RmlExecutionReport
    lateinit var baseIRI: String
    lateinit var mappingFile: Path
    fun doMain(
        mappingFilePath: String,
        outputFilePath: String?,
        outputFormat: Lang?,
        reportFilePath: String?,
        baseIRI: String,
        currentWorkingDirectory: Path?
    ): Int {

        Main.report = RmlExecutionReport()
        Main.baseIRI = baseIRI
        Main.mappingFile = Path.of(mappingFilePath)

        try {
            // Parse the mapping file
            val parser = Parse()
            var triplesMaps: MutableList<TriplesMap>
            try {
                triplesMaps = parser.parseMappingFile(mappingFile, currentWorkingDirectory)
            } catch (e: Exception) {
                throw BurpException(
                    RmlError(
                        e.message ?: "Unknown Error while Parsing ${mappingFilePath}",
                        null,
                        RER.RDFMappingSyntaxError,
                        exception = e
                    )
                ) //TODO Improve parsing error origin
            }
            if (triplesMaps.isEmpty()) report.errors.add(NoTriplesMap())

            // Wire AST tree
            val document = MappingDocument(triplesMaps)
            PlanWiring.wire(document)
            report.executionPlan = document

            report.statistics.generatedStatementPerTriplesMap =
                triplesMaps.associateWith { it.countGeneratedStatements }

            val statements = document.generate()

            report.statistics.generatedStatementPerTriplesMap =
                triplesMaps.associateWith { it.countGeneratedStatements }

            val lang = outputFormat
                ?: outputFilePath?.let(::pathnameToLang)
                ?: Lang.NQ

            if (outputFilePath != null) {
                FileOutputStream(outputFilePath).use { output ->
                    writeStatements(output, statements, lang)
                }
            } else {
                writeStatements(System.out, statements, lang)
            }
        } catch (e: BurpException) {
            report.errors.add(e.error)
        } catch (e: Exception) {
            report.errors.add(UnexpectedError(e))
        } finally {
            println(generateTextReport(report))
            if (reportFilePath != null) {
                generateRdfReport(report, reportFilePath)
            }
        }

        return if (report.errors.isEmpty()) 0 else 1
    }

    /**
     * Convert the list of statements into a Jena Dataset
     */
    private fun writeStatements(output: OutputStream, statements: List<RdfStatement>, lang: Lang) {
        if (lang == Lang.NQ) {
            NQuadsWriter.write(output, statements)
            report.statistics.generatedStatements = statements.size.toLong()
            return
        }

        val ds = generateDataset(statements)
        if (RDFLanguages.isQuads(lang)) {
            RDFDataMgr.write(output, ds, lang)
        } else {
            RDFDataMgr.write(output, ds.defaultModel, lang)
            report.errors.add(
                RmlError(
                    "Output language $lang does not support dataset, writing the default graph only.",
                    null,
                    RER.Warning
                )
            )
        }
    }

    private fun generateDataset(statements: List<RdfStatement>): Dataset {
        val ds = DatasetFactory.create()
        fun getModel(g: IRITerm?): Model {
            if (g == null) return ds.defaultModel
            if (g.uri == RML.defaultGraph.uri) return ds.defaultModel
            return ds.getNamedModel(g.uri)
        }

        // Blank Node scope is the RDF Store (not the graph)
        val bnodeMap = mutableMapOf<String, Resource>()

        for (stmt in statements) {
            val model = getModel(stmt.graph)
            val s = when (val sub = stmt.subject) {
                is IRITerm -> ResourceFactory.createResource(sub.uri)
                is BlankNodeTerm -> bnodeMap.getOrPut(sub.id) { ResourceFactory.createResource() }
                else -> throw RuntimeException("Subject must be URI or BlankNode")
            }
            val p = ResourceFactory.createProperty(stmt.predicate.uri)
            val o = when (val obj = stmt.`object`) {
                is IRITerm -> ResourceFactory.createResource(obj.uri)
                is BlankNodeTerm -> bnodeMap.getOrPut(obj.id) { ResourceFactory.createResource() }
                is LiteralTerm -> {
                    if (obj.language != null) {
                        ResourceFactory.createLangLiteral(obj.value, obj.language)
                    } else if (obj.datatype != null) {
                        ResourceFactory.createTypedLiteral(obj.value, BaseDatatype(obj.datatype.uri))
                    } else {
                        ResourceFactory.createTypedLiteral(obj.value)
                    }
                }

                else -> throw RuntimeException("Unsupported object term $obj")
            }
            model.add(s, p, o)
        }

        // Count all statements for statistics
        report.statistics.generatedStatements = (ds.defaultModel.size()
                + ds.listModelNames().asSequence().sumOf { ds.getNamedModel(it).size() })

        return ds
    }
}
