package burp.model.gathermap

import burp.model.*
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import burp.vocabularies.RML
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import rdfobjectloader.annotations.RdfProperty


class GatherMap : PlanNode {

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(gatherMaps)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    @RdfProperty(Rml.allowEmptyListAndContainer)
    var allowEmptyListAndContainer: Boolean = false
    
    @RdfProperty(Rml.gatherAs)
    var gatherAs: Resource? = null
    
    @RdfProperty(Rml.strategy)
    var strategy: Resource? = RML.append
    var strategyOrigin: Origin? = null

    @RdfProperty(Rml.gather)
    var gatherMaps: MutableList<TermGenerator> = mutableListOf()

    companion object {
        private var idCounter = 0L
        fun nextId() = BlankNodeTerm("gathermap-${idCounter++}")
    }

    fun generateTerms(
        i: Iteration, explicitIds: List<BlankNodeOrIRI>? = null
    ): List<CollectionOrContainerTerm> {
        val superCollection: List<List<Term>> = gatherMaps.map { it.generateTerms(i) }

        // 2. Determine the sets of items to process based on the strategy
        val itemSets: List<List<Term>> = when (strategy) {
            RML.append -> {
                // Flatten all results into a single list, wrapped in a list to iterate once
                listOf(superCollection.flatten())
            }

            RML.cartesianProduct -> {
                // Generate all combinations of the candidates
                cartesianProduct(superCollection)
            }

            else -> throw BurpException(
                RmlError(
                    "Unknown strategy: $strategy, choose either ${RML.append} or ${RML.cartesianProduct}.",
                    strategyOrigin,
                    RER.OutOfSpec,
                    null,
                    mapOf(RML.strategy to strategy)
                )
            )
        }

        // 3. Generate a container/collection for each set of items
        val results = mutableListOf<CollectionOrContainerTerm>()

        for (items in itemSets) {
            val (idsToUse, idGenerated) = explicitIds?.let { it to false } ?: (listOf(nextId()) to true)
            for (id in idsToUse) {
                if (items.isEmpty() && !allowEmptyListAndContainer) continue

                val itemsMutable = items.toMutableList()
                val container = when (gatherAs) {
                    RDF.Alt -> RdfAltTerm(itemsMutable, id, idGenerated)
                    RDF.Bag -> RdfBagTerm(itemsMutable, id, idGenerated)
                    RDF.Seq -> RdfSeqTerm(itemsMutable, id, idGenerated)
                    else -> RdfListTerm(itemsMutable, id, idGenerated) // Default to List
                }
                results.add(container)
            }
        }

        return results
    }

    private fun <T> cartesianProduct(lists: List<List<T>>): List<List<T>> {
        var result: MutableList<List<T>> = mutableListOf(emptyList())
        for (list in lists) {
            val currentResult = mutableListOf<List<T>>()
            for (item in list) {
                for (combination in result) {
                    val newCombination = combination + item
                    currentResult.add(newCombination)
                }
            }
            result = currentResult
        }
        return result
    }
}
