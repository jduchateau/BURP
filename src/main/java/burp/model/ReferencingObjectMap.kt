package burp.model

import burp.model.TemplateReferenceSafety.Unsafe
import burp.model.gathermap.GatherMapMixin
import burp.model.gathermap.SubGraph
import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode

class ReferencingObjectMap : GatherMap {
    var parent: TriplesMap? = null
    var joinConditions = mutableListOf<JoinCondition>()

    var gatherMap: GatherMapMixin? = null

    override fun isGatherMap(): Boolean {
        return gatherMap != null
    }

    override fun generateGatherMapGraphs(i: Iteration, baseIRI: String): List<SubGraph> {
        if (!isGatherMap()) throw RuntimeException("Trying to process a non-gathermap as gathermap")

        val g = mutableListOf<SubGraph>()

        try {
            for (n in generateTerms(i, baseIRI)) {
                val sg = SubGraph(n, ModelFactory.createDefaultModel())
                g.add(sg)
            }
        } catch (e: BurpException) {
            throw RuntimeException(e)
        }

        return g
    }

    @Throws(BurpException::class)
    override fun generateTerms(i: Iteration, baseIRI: String): List<RDFNode> {
        // If there are no join conditions, then we generate resources
        // from the child iteration. This is only guaranteed to work
        // for logical sources of the same type or if the parent triple
        // map' subject map only uses simple references.
        if (joinConditions.isEmpty()) {
            return parent!!.subjectMap.generateTerms(i, baseIRI)
        } else {
            val list = mutableListOf<RDFNode>()
            val parentIterator = parent!!.logicalSource?.iterator() ?: throw BurpException(
                RmlError(
                    "Constant triples map in referencing object map $this for triples map $parent (without logical source) are not supported.",
                    null,
                    RER.UnsupportedMapping
                )
            )
            parentIterator.forEach { parentIteration ->
                // Expression Maps are multi-valued. We thus need
                // For each join condition at least one match.
                var ok = true
                for (jc in joinConditions) {
                    val valuesChild = jc.childMap.generateValues(i, baseIRI, Unsafe)
                    val valuesParent = jc.parentMap.generateValues(parentIteration, baseIRI, Unsafe)

                    if (valuesChild.distinct().filter { valuesParent.contains(it) }.toSet().isEmpty()) {
                        // No match, break.
                        ok = false
                        break
                    }
                }

                if (ok) {
                    list.addAll(parent!!.subjectMap.generateTerms(parentIteration, baseIRI))
                }
            }

            return list
        }
    }
}