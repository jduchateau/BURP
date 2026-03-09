package burp

import burp.model.TriplesMap
import burp.model.gathermap.SubGraph
import burp.parse.Parse
import burp.reporting.*
import burp.util.BURPConfiguration
import burp.vocabularies.BURP
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.github.ajalt.clikt.core.main
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFLanguages.pathnameToLang
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.RDF
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
            report.executionPlan = triplesMaps
            report.statistics.generatedStatementPerTriplesMap =
                triplesMaps.associateWith { it.countGeneratedStatements }

            val ds = generate(triplesMaps, conf.baseIRI)

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

    @Throws(BurpException::class)
    private fun generate(triplesMaps: MutableList<TriplesMap>, givenBaseIRI: String): Dataset {
        val ds = DatasetFactory.create()

        // Execute the triples maps
        for (tm in triplesMaps) {
            val baseIRI = tm.baseIRI ?: givenBaseIRI

            val subjectMap = tm.subjectMap

            val subjectGraphMaps = subjectMap.graphMaps

            val logicalSource = tm.logicalSource ?: throw BurpException(
                RmlError(
                    "Constant Triples Map $tm (without logical source) are not supported.",
                    null,
                    RER.UnsupportedMapping
                )
            )
            val iter = logicalSource.iterator()
            // Iterates source records; generates and stores triples
            while (iter.hasNext()) {
                val i = iter.next()

                val subjectGraphs = subjectGraphMaps.flatMap { gm -> gm.generateTerms(i, baseIRI) }.toSet()

                val targetGraphsForSubjectMap =
                    if (subjectGraphMaps.isEmpty()) setOf(RML.defaultGraph) else subjectGraphs

                val subjects = mutableListOf<RDFNode>()
                if (!subjectMap.isGatherMap()) {
                    subjects.addAll(subjectMap.generateTerms(i, baseIRI))
                } else {
                    for (subgraph in subjectMap.generateGatherMapGraphs(i, baseIRI)) {
                        subjects.add(subgraph.node!!)
                        addToGraphs(ds, targetGraphsForSubjectMap, subgraph, tm)
                    }
                }

                // For each subject in subjects and each class in classes,
                // add triples to the output dataset as follows:
                // subject: subject
                // predicate: rdf:type
                // object: class
                // target graphs: If sgm is empty: rr:defaultgraph; otherwise: subject_graphs
                storeTriplesOfSubjectMaps(ds, subjectMap.classes, subjects, targetGraphsForSubjectMap, tm)

                // For each predicate-object map of the triples map, apply the following steps:
                // Let predicates be the set of generated RDF terms that result
                // from applying each of the predicate-object map's predicate maps to i
                // Let objects be the set of generated RDF terms that result from applying each
                // of the predicate-object map's object maps (but not referencing object maps)
                // to i
                // Let pogm be the set of graph maps of the predicate-object map
                // Let pogs be the set of generated RDF terms that result from applying each
                // graph map in pogm to i
                // For each possible combination <s, p, o> in subjects X predicates X objects,
                // add triples to the output dataset as follows:
                // s: subject
                // p: predicate
                // o: object
                // Target graphs: If sgm and pogm are empty: rr:defaultGraph; otherwise:
                // union of subject_graphs and predicate-object_graphs
                for (pom in tm.predicateObjectMaps) {
                    val predicateObjectGraphs = pom.graphMaps.flatMap { it.generateTerms(i, baseIRI) }.toSet()

                    val graphs = if (subjectGraphMaps.isEmpty() && pom.graphMaps.isEmpty()) setOf(RML.defaultGraph)
                    else subjectGraphs + predicateObjectGraphs

                    val predicates = pom.predicateMaps.flatMap { it.generateTerms(i, baseIRI) }.toList()

                    val objects = mutableListOf<RDFNode>()
                    for (om in pom.objectMaps) {
                        if (!om.isGatherMap()) {
                            objects.addAll(om.generateTerms(i, baseIRI))
                        } else {
                            for (subgraph in om.generateGatherMapGraphs(i, baseIRI)) {
                                objects.add(subgraph.node!!)
                                addToGraphs(ds, graphs, subgraph, tm)
                            }
                        }
                    }

                    for (rom in pom.refObjectMaps) {
                        objects.addAll(rom.generateTerms(i, baseIRI))
                    }

                    storeTriples(ds, subjects, predicates, objects, graphs, tm)
                }
            }
        }

        removeJunk(ds)

        // Count all statements
        report.statistics.generatedStatements = (ds.defaultModel.size()
                + ds.listModelNames().asSequence().sumOf { ds.getNamedModel(it).size() })

        return ds
    }

    private fun removeJunk(ds: Dataset) {
        removeJunk(ds.defaultModel)
        ds.listModelNames().iterator().forEach { removeJunk(ds.getNamedModel(it)) }
    }

    /**
     * Removes junk statements from the given model.
     * - Remove temporary synthetic rml:list annotation
     * - If a list is empty (= has no rdf:first), point it to rdf:nil directly
     */
    private fun removeJunk(model: Model) {
        val stmts = model.listStatements(null, RDF.type, BURP.list).toList()
        for (stmt in stmts) {
            model.remove(stmt)

            if (!stmt.subject.hasProperty(RDF.first)) {
                ResourceUtils.renameResource(stmt.subject, RDF.nil.toString())
            }
        }
    }

    private fun storeTriples(
        ds: Dataset,
        subjects: List<RDFNode>,
        predicates: List<RDFNode>,
        objects: List<RDFNode>,
        graphs: Collection<RDFNode>,
        forTriplesMap: TriplesMap
    ) {
        for (s in subjects) {
            val sr = s.asResource()
            for (p in predicates) {
                val pp = ResourceFactory.createProperty(p.asResource().uri)
                for (o in objects) {
                    for (g in graphs) {
                        getModel(ds, g).add(sr, pp, o)
                        forTriplesMap.countGeneratedStatements++
                    }
                }
            }
        }
    }

    private fun storeTriplesOfSubjectMaps(
        ds: Dataset,
        classes: List<Resource>,
        subjects: List<RDFNode>,
        graphs: Set<RDFNode>,
        forTriplesMap: TriplesMap
    ) {
        for (s in subjects) {
            val sr = s.asResource()
            for (c in classes) {
                for (g in graphs) {
                    getModel(ds, g).add(sr, RDF.type, c)
                    forTriplesMap.countGeneratedStatements++
                }
            }
        }
    }

    private fun addToGraphs(ds: Dataset, graphs: Set<RDFNode>, subgraph: SubGraph, forTriplesMap: TriplesMap) {
        for (graph in graphs) {
            val g = getModel(ds, graph)
            val r = subgraph.node!!.asResource()

            if (subgraph.isList) {
                g.add(r, RDF.type, BURP.list)
                // Not counting this statement as it is solly for internal use.

                try {
                    val l = g.getList(r)
                    var sub = subgraph.model!!.getList(r)

                    val elements = sub.iterator().toList()
                    for (e in elements) {
                        l.add(e)
                        forTriplesMap.countGeneratedStatements++
                    }

                    while (!sub.isEmpty()) {
                        sub = sub.removeHead()
                    }

                    g.add(subgraph.model)
                    forTriplesMap.countGeneratedStatements += subgraph.model!!.size()
                } catch (e: Exception) {
                    // List did not exist, so we can just add it
                    g.add(subgraph.model)
                    forTriplesMap.countGeneratedStatements += subgraph.model!!.size()
                }
            } else {
                var c: Container? = null
                var sub: Container? = null
                if (subgraph.isAlt) {
                    g.add(r, RDF.type, RDF.Alt)
                    c = g.getAlt(r)
                    sub = subgraph.model!!.getAlt(r)
                } else if (subgraph.isBag) {
                    g.add(r, RDF.type, RDF.Bag)
                    c = g.getAlt(r)
                    sub = subgraph.model!!.getBag(r)
                } else if (subgraph.isSeq) {
                    g.add(r, RDF.type, RDF.Seq)
                    c = g.getAlt(r)
                    sub = subgraph.model!!.getSeq(r)
                }
                checkNotNull(sub)
                checkNotNull(c)
                forTriplesMap.countGeneratedStatements++ // TODO, check if we will not count rdf:type twice

                // Now amend everything so that
                // we append the containers
                val elements = sub.iterator().toList()
                for (e in elements) {
                    c.add(e)
                    forTriplesMap.countGeneratedStatements++
                }

                for (s in sub.listProperties().toList()) {
                    if (s.subject == r
                        && (s.predicate.uri.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#_")
                                || s.predicate.equals(RDF.type))
                    )
                        s.remove()
                }

                // We add all the remaining triples
                g.add(subgraph.model)
                forTriplesMap.countGeneratedStatements += subgraph.model!!.size()
            }
        }
    }

    private fun getModel(ds: Dataset, g: RDFNode): Model {
        if (g == RML.defaultGraph) return ds.defaultModel
        return ds.getNamedModel(g.asResource())
    }
}
