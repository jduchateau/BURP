package burp.model

import burp.Main
import burp.reporting.Origin
import org.apache.jena.rdf.model.Resource

class TriplesMap(var subject: Resource?) : PlanNode, BaseIRIScope, LocalReferenceScope, LogicalTargetScope {
    var logicalSource: AbstractLogicalSource? = null
    lateinit var subjectMap: SubjectMap
    var predicateObjectMaps = mutableListOf<PredicateObjectMap>()
    var baseIRI: String? = null
    override val logicalTargets: MutableSet<LogicalTarget> = mutableSetOf()

    override fun getBaseIri() = baseIRI ?: Main.baseIRI

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (logicalSource != null) yield(logicalSource!!)
        yield(subjectMap)
        yieldAll(predicateObjectMaps)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    var countGeneratedStatements: Long = 0

    private fun unionTargets(vararg targetSets: Set<LogicalTarget>?): Set<LogicalTarget> {
        val nonNullSets = targetSets.filterNotNull().filter { it.isNotEmpty() }
        if (nonNullSets.isEmpty()) return emptySet()
        val union = mutableSetOf<LogicalTarget>()
        for (set in nonNullSets) {
            union.addAll(set)
        }
        return union
    }

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
                    stmts.add(RdfStatementSubjectGraph(s, g, unionTargets(s.targets, g?.targets)))
                }
            }
            for (c in subjectMap.classes) {
                // c targets inherited from subjectMap
                val classTargets = subjectMap.getEffectiveTargets()
                for (g in targetGraphsForSubjectMap) {
                    stmts.add(
                        RdfStatement(
                            s, IRITerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), IRITerm(c.uri, classTargets), g, unionTargets(s.targets, classTargets, g?.targets)
                        )
                    )
                    countGeneratedStatements++
                }
            }
        }

        // 2. Generate and store PredicateObjects
        for (pom in predicateObjectMaps) {
            val generatedPoms = pom.generate(i)
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
                        val finalTargets = unionTargets(s.targets, po.targets, g?.targets)
                        stmts.add(RdfStatement(s as BlankNodeOrIRI, po.predicate, po.`object`, g, finalTargets))
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
