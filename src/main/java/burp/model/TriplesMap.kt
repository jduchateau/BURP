package burp.model

import burp.Main
import burp.model.gathermaputil.SubGraph
import burp.vocabularies.RML
import org.apache.jena.query.Dataset
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF

class TriplesMap(val iri: Resource?) :IPlanNode {
    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null

    lateinit var logicalSource: LogicalSource
    lateinit var subjectMap: SubjectMap

    /// Maintains a reference to ReferencingObjectMap having this triples map as parent.
    var parentOf: Set<ReferencingObjectMap> = setOf()

    @JvmField
    var predicateObjectMaps = listOf<PredicateObjectMap>()

    fun generateInto(baseIRI: String?, ds: Dataset) {

        println("Processing $this")

        // Let sm be the subject map of the triples map
        val sm = this.subjectMap

        // Let sgm be the set of graph maps of subject maps
        val sgm = sm.graphMaps

        // For each iteration i in iterations, apply the following steps:
        val iter = this.logicalSource.iterator()
        while (iter.hasNext()) {
            val i = iter.next()

            // Let sgs be the set of the generated RDF terms
            // that result from applying each term map in sgm to i
            val sgs = if (sgm.isEmpty()) mutableListOf<RDFNode>(RML.defaultGraph) else mutableListOf()
            for (gm in sgm) {
                sgs.addAll(gm.generateTerms(i, baseIRI))
            }

            // Let subjects be the generated RDF terms that result from applying sm to i
            val subjects: MutableList<RDFNode> = mutableListOf()

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
            storeTriplesOfSubjectMaps(ds, sm.classes, subjects, sgs)

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
            for (pom in this.predicateObjectMaps) {
                val pogs: MutableList<RDFNode> = ArrayList<RDFNode>()
                for (gm in pom.graphMaps) {
                    pogs.addAll(gm.generateTerms(i, baseIRI))
                }

                var graphs = listOf<RDFNode>(RML.defaultGraph)
                // If sgm and pogm are empty: rr:defaultGraph (see line above)
                if (!sgm.isEmpty() || !pogs.isEmpty()) {
                    // otherwise: union of subject_graphs and predicate-object_graphs
                    // we do an additional test as sgs contains rml:defaultGraph if sgm is empty
                    // we do not want to include that
                    pogs.addAll(if (!sgm.isEmpty()) sgs else ArrayList<RDFNode>())
                    graphs = pogs
                }

                val predicates: MutableList<RDFNode> = ArrayList<RDFNode>()
                for (pm in pom.predicateMaps) {
                    predicates.addAll(pm.generateTerms(i, baseIRI))
                }
                println("Predicates for POM $pom: $predicates")

                val objects: MutableList<RDFNode?> = ArrayList<RDFNode?>()

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

                storeTriples(ds, subjects, predicates, objects, graphs)

            }

            if (Main.conf.indexedJoins)
                parentOf.forEach { prepareParentJoinIndexes(it, i, subjects) }

        }
    }

    fun prepareParentJoinIndexes(rom: ReferencingObjectMap, iteration: Iteration, subjects: MutableList<RDFNode>) {
        println("Preparing join indexes for $rom of ${rom.parentTriplesMap}")
        rom.joinConditions.forEach { jc ->
            val parentValues = jc.parentMap.generateValues(iteration)
            parentValues.forEach { value ->
                val existing = jc.parentValsToSubjects[value]
                if (existing != null) {
                    jc.parentValsToSubjects[value] = existing + subjects.map { it.asResource() }
                } else {
                    jc.parentValsToSubjects[value] = subjects.map { it.asResource() }.toSet()
                }
            }
        }
    }

    private fun storeTriples(
        ds: Dataset,
        subjects: List<RDFNode>,
        predicates: List<RDFNode>,
        objects: List<RDFNode?>,
        graphs: List<RDFNode>,
    ) {
        for (s in subjects) {
            val sr = s.asResource()
            for (p in predicates) {
                val pp = ResourceFactory.createProperty(p.asResource().uri)
                for (o in objects) for (g in graphs) getModel(ds, g).add(sr, pp, o)
            }
        }
    }

    private fun storeTriplesOfSubjectMaps(
        ds: Dataset,
        classes: List<Resource>,
        subjects: List<RDFNode>,
        graphs: List<RDFNode>
    ) {
        for (s in subjects) {
            val sr = s.asResource()

            for (c in classes) for (g in graphs) getModel(ds, g).add(sr, RDF.type, c)
        }
    }

    private fun addToGraphs(ds: Dataset, graphs: List<RDFNode>, subgraph: SubGraph) {
        for (graph in graphs) {
            val g: Model = getModel(ds, graph)
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

                    while (!sub.isEmpty) {
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
                } else if (subgraph.isBag) {
                    g.add(r, RDF.type, RDF.Bag)
                    c = g.getAlt(r)
                    sub = subgraph.model.getBag(r)
                } else if (subgraph.isSeq) {
                    g.add(r, RDF.type, RDF.Seq)
                    c = g.getAlt(r)
                    sub = subgraph.model.getSeq(r)
                }

                // Now amend everything so that
                // we append the containers
                sub!!.iterator().forEach { c!!.add(it) }

                sub.listProperties().forEach {
                    if (it.subject == r && it.predicate.uri.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#_"))
                        it.remove()
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

    override fun toString(): String {
        return super.toString() + "(${iri})"
    }
}
