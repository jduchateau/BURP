package burp

import burp.model.TriplesMap
import burp.model.gathermaputil.SubGraph
import burp.parse.Parse
import burp.reporting.*
import burp.util.BURPConfiguration
import burp.vocabularies.RML
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.RDF
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

object Main {
    private val def: List<RDFNode> = listOf(RML.defaultGraph)

    fun main(args: Array<String>) {
        val cwd = Paths.get("").toAbsolutePath()
        val exit = doMain(args, cwd)
        println("System exiting with code: $exit")
        exitProcess(exit)
    }

    fun doMain(args: Array<String>): Int {
        val cwd = Paths.get("").toAbsolutePath()
        return doMain(args, cwd)
    }

    // Hack to quickly get the config from anywhere
    var conf: BURPConfiguration? = null
    var report: RmlExecutionReport? = null
    fun doMain(args: Array<String>, currentWorkingDirectory: Path?): Int {
        report = RmlExecutionReport()
        try {
            // Process the configuration file
            conf = BURPConfiguration(args)

            // Parse the mapping file
            val parser = Parse()
            var triplesMaps: MutableList<TriplesMap>
            try {
                triplesMaps = parser.parseMappingFile(Paths.get(conf!!.mappingFile), currentWorkingDirectory)
            } catch (e: Exception) {
                throw BurpException(RDFMappingSyntaxError(e.message!!, null)) //TODO Improve parsing error origin
            }
            if (triplesMaps.isEmpty()) report!!.errors.add(NoTriplesMap())
            report!!.executionPlan = triplesMaps

            val ds = generate(triplesMaps, conf!!.baseIRI)

            if (conf!!.outputFile != null) RDFDataMgr.write(FileOutputStream(conf!!.outputFile), ds, Lang.NQ)
            else RDFDataMgr.write(System.out, ds, Lang.NQ)

            // It all went well, thus return 0
        } catch (e: BurpException) {
            report!!.errors.add(e.error)
        } catch (e: Exception) {
            report!!.errors.add(UnexpectedError(e))
        } finally {
            println(generateTextReport(report!!))
            // TODO
            // if (conf != null && conf.reportFile != null) {
            //     RdfReportGeneratorKt.generateRdfReport(report, conf.reportFile);
            // }
        }

        return if (report!!.errors.isEmpty()) 0 else 1
    }

    @Throws(BurpException::class)
    private fun generate(triplesMaps: MutableList<TriplesMap>, givenBaseIRI: String?): Dataset {
        val ds = DatasetFactory.create()

        // Execute the triples maps
        for (tm in triplesMaps) {
            val baseIRI = tm.baseIRI ?: givenBaseIRI

            // Let sm be the subject map of the triples map
            val sm = tm.subjectMap

            // Let sgm be the set of graph maps of subject maps
            val sgm = sm.graphMaps

            // For each iteration i in iterations, apply the following steps:
            val iter = tm.logicalSource.iterator()
            while (iter.hasNext()) {
                val i = iter.next()

                // Let sgs be the set of the generated RDF terms
                // that result from applying each term map in sgm to i
                val sgs = if (sgm.isEmpty()) def else sgm.flatMap { gm -> gm.generateTerms(i, baseIRI) }.toList()

                // Let subjects be the generated RDF terms that result from applying sm to i
                val subjects = mutableListOf<RDFNode>()

                if (!sm.isGatherMap) {
                    subjects.addAll(sm.generateTerms(i, baseIRI))
                } else {
                    for (subgraph in sm.generateGatherMapGraphs(i, baseIRI)) {
                        subjects.add(subgraph.node)
                        addToGraphs(ds, sgs, subgraph)
                    }
                }

                // For each subject in subjects and each class in classes,
                // add triples to the output dataset as follows:
                // subject: subject
                // predicate: rdf:type
                // object: class
                // target graphs: If sgm is empty: rr:defaultgraph; otherwise: subject_graphs
                storeTriplesOfSubjectMaps(ds, sm.classes, subjects, sgs, tm)

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
                    val pogs = mutableListOf<RDFNode>()
                    for (gm in pom.graphMaps) {
                        pogs.addAll(gm.generateTerms(i, baseIRI))
                    }

                    var graphs = def
                    // If sgm and pogm are empty: rr:defaultGraph (see line above)
                    if (!sgm.isEmpty() || !pogs.isEmpty()) {
                        // otherwise: union of subject_graphs and predicate-object_graphs
                        // we do an additional test as sgs contains rml:defaultGraph if sgm is empty
                        // we do not want to include that
                        pogs.addAll(if (!sgm.isEmpty()) sgs else listOf())
                        graphs = pogs
                    }

                    val predicates = mutableListOf<RDFNode>()
                    for (pm in pom.predicateMaps) {
                        predicates.addAll(pm.generateTerms(i, baseIRI))
                    }

                    val objects = mutableListOf<RDFNode>()

                    for (om in pom.objectMaps) {
                        if (!om.isGatherMap) {
                            objects.addAll(om.generateTerms(i, baseIRI))
                        } else {
                            for (subgraph in om.generateGatherMapGraphs(i, baseIRI)) {
                                objects.add(subgraph.node)
                                addToGraphs(ds, graphs, subgraph)
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

    private fun storeTriples(
        ds: Dataset,
        subjects: List<RDFNode>,
        predicates: List<RDFNode>,
        objects: List<RDFNode>,
        graphs: List<RDFNode>,
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
        graphs: List<RDFNode>,
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

    private fun addToGraphs(ds: Dataset, graphs: List<RDFNode>, subgraph: SubGraph) {
        for (graph in graphs) {
            val g = getModel(ds, graph)
            val r = subgraph.node.asResource()

            if (subgraph.isList) {
                g.add(r, RDF.type, RML.list)

                try {
                    val l = g.getList(r)
                    var sub = subgraph.model.getList(r)

                    val elements = sub.iterator().toList()
                    for (e in elements) {
                        l.add(e)
                    }

                    while (!sub.isEmpty()) {
                        sub = sub.removeHead()
                    }

                    g.add(subgraph.model)
                } catch (e: Exception) {
                    // List did not exist, so we can just add it
                    g.add(subgraph.model)
                }
            } else {
                var c: Container? = null
                var sub: Container? = null
                if (subgraph.isAlt) {
                    g.add(r, RDF.type, RDF.Alt)
                    c = g.getAlt(r)
                    sub = subgraph.model.getAlt(r)
                } else if (subgraph.isBag()) {
                    g.add(r, RDF.type, RDF.Bag)
                    c = g.getAlt(r)
                    sub = subgraph.model.getBag(r)
                } else if (subgraph.isSeq()) {
                    g.add(r, RDF.type, RDF.Seq)
                    c = g.getAlt(r)
                    sub = subgraph.model.getSeq(r)
                }
                checkNotNull(sub)
                checkNotNull(c)

                // Now amend everything so that
                // we append the containers
                val elements = sub.iterator().toList()
                for (e in elements) c.add(e)

                val iter = sub.listProperties()
                while (iter.hasNext()) {
                    val s = iter.next()
                    if (s.getSubject() == r) if (s.getPredicate().getURI()
                            .startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#_")
                    ) iter.remove()
                }

                // We add all the remaining triples
                g.add(subgraph.model)
            }
        }
    }

    private fun getModel(ds: Dataset, g: RDFNode): Model {
        if (g == RML.defaultGraph) return ds.defaultModel
        return ds.getNamedModel(g.asResource())
    }
}
