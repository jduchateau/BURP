package burp

import burp.model.TriplesMap
import burp.parse.Parse
import burp.util.BURPConfiguration
import burp.vocabularies.RML
import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.RDF
import java.io.File
import java.io.FileOutputStream
import kotlin.system.exitProcess

object Main {
    lateinit var conf: BURPConfiguration
    lateinit var prefixes: Map<String, String>

    @JvmStatic
    fun main(args: Array<String>) = runBlocking<Unit> {
        val exit = doMain(args)
        exitProcess(exit)
    }

    @JvmStatic
    fun doMain(args: Array<String>): Int {
        try {
            // Process the configuration file
            conf = BURPConfiguration(args)

            // Parse the mapping file
            val parser = Parse()
            val triplesMaps = parser.parseMappingFile(conf.mappingFile)

            val ds = generate(triplesMaps, conf.baseIRI)

            val out = if (conf.outputFile != null) {
                File(conf.outputFile).parentFile?.mkdirs()
                FileOutputStream(conf.outputFile)
            } else System.out

            RDFDataMgr.write(out, ds, Lang.NQ)


            val stmtCount =
                ds.defaultModel.size() + ds.listModelNames().asSequence().map { ds.getNamedModel(it).size() }.sum()
            println("Generated $stmtCount RDF statements.")

            // It all went well, thus return 0
            return 0
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println(e.message)
            return 1
        }
    }

    suspend fun liveMain() {
        coroutineScope {
            val scope = CoroutineScope(coroutineContext)
            val watcher = KfsDirectoryWatcher(scope)
            watcher.add(conf.mappingFile)
            val out = if (conf.outputFile != null) {
                File(conf.outputFile).parentFile?.mkdirs()
                FileOutputStream(conf.outputFile)
            } else System.out
            launch {
                watcher.onEventFlow.collect { event ->
                    println("Event received: $event")
                    try {
                        val parser = Parse()
                        val triplesMaps = parser.parseMappingFile(conf.mappingFile)

                        val ds = generate(triplesMaps, conf.baseIRI)
                        RDFDataMgr.write(out, ds, Lang.NQ)
                        val stmtCount =
                            ds.defaultModel.size() + ds.listModelNames().asSequence()
                                .map { ds.getNamedModel(it).size() }.sum()
                        println("Generated $stmtCount RDF statements.")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        System.err.println(e.message)
                    }
                }
            }
            watcher.removeAll()
            watcher.close()
        }
    }

    fun generate(triplesMaps: List<TriplesMap>, baseIRI: String?): Dataset {
        val ds = DatasetFactory.create()

        println("Generate TriplesMap:")
        triplesMaps.forEachIndexed { i, tm -> println("$i. $tm") }
        // Execute the triples maps
        for (tm in triplesMaps) tm.generateInto(baseIRI, ds)

        removeJunk(ds)
        return ds
    }

    private fun removeJunk(ds: Dataset) {
        removeJunk(ds.defaultModel)
        val iter = ds.listModelNames()
        while (iter.hasNext()) removeJunk(ds.getNamedModel(iter.next()))
    }

    private fun removeJunk(model: Model) {
        val s = model.listStatements(null, RDF.type, RML.list)
        while (s.hasNext()) {
            val statement = s.next()
            s.remove()

            val l = statement.subject
            if (!l.hasProperty(RDF.first)) {
                ResourceUtils.renameResource(l, RDF.nil.toString())
            }
        }
    }

}