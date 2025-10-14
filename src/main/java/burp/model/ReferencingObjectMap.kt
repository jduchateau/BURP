package burp.model

import burp.Main
import burp.model.gathermaputil.GatherMapMixin
import burp.model.gathermaputil.SubGraph
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode

class ReferencingObjectMap : GatherMap, IPlanNode {
    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null

    lateinit var parentTriplesMap: TriplesMap
    var joinConditions: List<JoinCondition> = listOf()

    var gatherMap: GatherMapMixin? = null
    override fun isGatherMap(): Boolean {
        return gatherMap != null
    }

    override fun generateGatherMapGraphs(i: Iteration?, baseIRI: String?): MutableList<SubGraph?> {
        assert(isGatherMap) { "Trying to process a non-gathermap as gathermap" }

        val graphs: MutableList<SubGraph?> = ArrayList()

        for (n in generateTerms(i, baseIRI)) {
            val sg = SubGraph(n, ModelFactory.createDefaultModel())
            graphs.add(sg)
        }

        return graphs
    }

    override fun generateTerms(childIteration: Iteration?, baseIRI: String?): List<RDFNode> {
        // If there are no join conditions, then we generate resources from the child iteration.
        // This is only guaranteed to work for logical sources of the same type
        // or if the parent triple map subject map only uses simple references.
        if (joinConditions.isEmpty()) {
            return parentTriplesMap.subjectMap.generateTerms(childIteration, baseIRI)
        } else {
            // If a referencing object map has multiple join conditions,
            // then all of them MUST be satisfied for the referencing object map to be evaluated.
            //
            // Expression Maps are multivalued, thus we need at least one match for each join condition.
            println("Processing $this")
            if (Main.conf.indexedJoins) {
                val parents = joinConditions.map { jc ->
                    val childValues = jc.childMap.generateValues(childIteration)
                    val subjects =
                        childValues.flatMap { childVal -> jc.parentValsToSubjects.getOrElse(childVal) { emptySet() } }
                    subjects.distinct()
                }
                return if (parents.any { it.isEmpty() }) emptyList() else parents.flatten().distinct()
            } else
                return parentTriplesMap.logicalSource.iterator().asSequence().flatMap { parentIteration ->
                    val matches = joinConditions.all { jc ->
                        val childValues = jc.childMap.generateValues(childIteration).toSet()
                        val parentValues = jc.parentMap.generateValues(parentIteration).toSet()
                        childValues.any { it in parentValues }
                    }
                    if (matches)
                        parentTriplesMap.subjectMap.generateTerms(parentIteration, baseIRI).asSequence()
                    else
                        emptySequence()
                }.toList()
        }
    }
}