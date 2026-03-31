package burp.model

import burp.Main.conf
import burp.reporting.Origin
import org.apache.jena.rdf.model.Resource

class TriplesMap(var subject: Resource?) : PlanNode, BaseIRIScope, LocalReferenceScope {
    var logicalSource: AbstractLogicalSource? = null
    lateinit var subjectMap: SubjectMap
    var predicateObjectMaps = mutableListOf<PredicateObjectMap>()
    var baseIRI: String? = null

    override fun getBaseIri() = baseIRI ?: conf.baseIRI

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (logicalSource != null) yield(logicalSource!!)
        yield(subjectMap)
        yieldAll(predicateObjectMaps)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    var countGeneratedStatements: Long = 0

    fun generate(i: Iteration): List<RdfStatementLike> {
        val tmBaseIRI = getBaseIri()

        val stmts = mutableListOf<RdfStatementLike>()
        val subjectGraphs =
            subjectMap.graphMaps.flatMap { it.generateTerms(i).filterIsInstance<IRITerm>() }.toSet()
        val targetGraphsForSubjectMap = if (subjectMap.graphMaps.isEmpty()) setOf(null) else subjectGraphs

        val subjects = subjectMap.generateTerms(i)

        // 1. Store matching classes over subjects
        for (s in subjects) {
            if (s !is BlankNodeOrIRI) continue
            if (s is CollectionOrContainerTerm) {
                for (g in targetGraphsForSubjectMap) {
                    stmts.add(RdfStatementSubjectGraph(s, g))
                }
            }
            for (c in subjectMap.classes) {
                for (g in targetGraphsForSubjectMap) {
                    stmts.add(
                        RdfStatement(
                            s, IRITerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), IRITerm(c.uri), g
                        )
                    )
                    countGeneratedStatements++
                }
            }
        }

        // 2. Generate and store PredicateObjects
        for (pom in predicateObjectMaps) {
            val generatedPoms = pom.generate(i, tmBaseIRI)
            for (s in subjects) {
                //if (s !is BlankNodeOrIRI) continue
                for (po in generatedPoms) {
                    // Combine subject graphs and POM graphs
                    val combinedGraphs = mutableSetOf<IRITerm?>()
                    if (subjectGraphs.isEmpty() && pom.graphMaps.isEmpty()) {
                        combinedGraphs.add(null)
                    } else {
                        combinedGraphs.addAll(subjectGraphs)
                        if (po.graph != null) {
                            combinedGraphs.add(po.graph)
                        }
                    }

                    for (g in combinedGraphs) {
                        stmts.add(RdfStatement(s as BlankNodeOrIRI, po.predicate, po.`object`, g))
                        countGeneratedStatements++
                    }
                }
            }
        }
        return stmts
    }

    override fun buildLocalReference(reference: String, origin: Origin) = logicalSource!!.buildExportedReference(reference, origin)
}

interface BaseIRIScope : PlanNode {
    fun getBaseIri(): String
}
