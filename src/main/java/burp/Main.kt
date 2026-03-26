package burp

import burp.model.*
import burp.parse.Parse
import burp.parse.PlanWiring
import burp.reporting.*
import burp.util.BURPConfiguration
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.github.ajalt.clikt.core.main
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

    // Hack to quickly get the config from anywhere
    lateinit var conf: BURPConfiguration
    lateinit var report: RmlExecutionReport
    fun doMain(args: Array<String>, currentWorkingDirectory: Path?): Int {
        report = RmlExecutionReport()
        try {
            // Process the configuration file
            conf = BURPConfiguration()
            conf.main(args)

            // Parse the mapping file
            val parser = Parse()
            var triplesMaps: MutableList<TriplesMap>
            try {
                triplesMaps = parser.parseMappingFile(Paths.get(conf.mappingFile), currentWorkingDirectory)
            } catch (e: Exception) {
                throw BurpException(
                    RmlError(
                        e.message ?: "Unknown Error while Parsing ${conf.mappingFile}",
                        null,
                        RER.RDFMappingSyntaxError,
                        exception = e
                    )
                ) //TODO Improve parsing error origin
            }
            if (triplesMaps.isEmpty()) report.errors.add(NoTriplesMap())
            
            // Wire AST tree 
            val document = burp.model.MappingDocument(triplesMaps)
            PlanWiring.wire(document)
            
            report.statistics.generatedStatementPerTriplesMap =
                triplesMaps.associateWith { it.countGeneratedStatements }

            val statements = document.generate()
            val ds = generateDataset(statements)

            report.statistics.generatedStatementPerTriplesMap =
                triplesMaps.associateWith { it.countGeneratedStatements }

            val outputFile = conf.outputFile
            if (outputFile != null) {
                val lang = conf.outputFormat
                    ?: pathnameToLang(outputFile)
                    ?: Lang.NQ

                if (RDFLanguages.isQuads(lang)) {
                    RDFDataMgr.write(FileOutputStream(outputFile), ds, lang)
                } else {
                    RDFDataMgr.write(FileOutputStream(outputFile), ds.defaultModel, lang)
                    report.errors.add(
                        RmlError(
                            "Output language $lang does not support dataset, writing the default graph only.",
                            null,
                            RER.Warning
                        )
                    )
                }
            } else {
                RDFDataMgr.write(System.out, ds, Lang.NQ)
            }

        } catch (e: BurpException) {
            report.errors.add(e.error)
        } catch (e: Exception) {
            report.errors.add(UnexpectedError(e))
        } finally {
            println(generateTextReport(report))
            val reportFile = conf.reportFile
            if (reportFile != null) {
                generateRdfReport(report, reportFile)
            }
        }

        return if (report.errors.isEmpty()) 0 else 1
    }

    /**
     * Convert the list of statements into a Jena Dataset
     */
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
                        ResourceFactory.createTypedLiteral(obj.value, org.apache.jena.datatypes.BaseDatatype(obj.datatype.uri))
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
