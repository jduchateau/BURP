package burp.model.gathermap

import burp.model.GatherMap
import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.BURP
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.google.common.collect.Lists
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.RDF


class GatherMapMixin {

    var allowEmptyListAndContainer: Boolean = false
    var gatherAs: Resource? = null
    var strategy: Resource? = RML.append
    var strategyOrigin: Origin? = null
    var gatherMaps: MutableList<GatherMap> = mutableListOf()

    fun generateGraphs(i: Iteration?, baseIRI: String?): List<SubGraph> {
        val superCollection: List<List<SubGraph>> = gatherMaps.map { tm ->
            if (tm.isGatherMap) tm.generateGatherMapGraphs(i, baseIRI)
            else tm.generateTerms(i, baseIRI).map { SubGraph(it, null) }
        }

        // 2. Determine the sets of items to process based on the strategy
        val itemSets: List<List<SubGraph>> = when (strategy) {
            RML.append -> {
                // Flatten all results into a single list, wrapped in a list to iterate once
                listOf(superCollection.flatten())
            }

            RML.cartesianProduct -> {
                // Generate all combinations of the candidates
                Lists.cartesianProduct(superCollection)
            }

            else -> throw BurpException(
                RmlError(
                    "Unknown strategy: $strategy, choose either ${RML.append} or ${RML.cartesianProduct}.",
                    strategyOrigin,
                    RER.OutOfSpec,
                    null,
                    mapOf(RML.strategy to strategy) //FIXME how to deal with out of spec context, we will not add all the mapping again, pointers are there for that ? huh
                )
            )
        }

        // 3. Generate a graph for each set of items
        return itemSets.mapNotNull { items ->
            val m = ModelFactory.createDefaultModel()
            val n = m.createResource()
            if (gatherAs == RDF.List) createList(m, n, items)
            else createContainer(m, n, items)
            if (!m.isEmpty()) SubGraph(n, m) else null
        }
    }


    private fun createList(m: Model, n: RDFNode, list: List<SubGraph>) {
        if (!list.isEmpty() || allowEmptyListAndContainer) {
            m.add(n.asResource(), RDF.type, BURP.list)

            for (sg in list) {
                // Adding the element to the container
                try {
                    val l = m.getList(n.asResource())
                    l.add(sg.node)
                } catch (e: Exception) {
                    m.add(n.asResource(), RDF.rest, RDF.nil)
                    m.add(n.asResource(), RDF.first, sg.node)
                }

                // Adding any triples around it into the model
                if (sg.model != null) m.add(sg.model)
            }
        }
    }

    private fun createContainer(m: Model, n: RDFNode, list: List<SubGraph>) {
        if (!list.isEmpty() || allowEmptyListAndContainer) {
            var c: Container? = null
            if (gatherAs == RDF.Alt) {
                m.add(n.asResource(), RDF.type, RDF.Alt)
                c = m.getAlt(n.asResource())
            } else if (gatherAs == RDF.Bag) {
                m.add(n.asResource(), RDF.type, RDF.Bag)
                c = m.getBag(n.asResource())
            } else if (gatherAs == RDF.Seq) {
                m.add(n.asResource(), RDF.type, RDF.Seq)
                c = m.getSeq(n.asResource())
            }

            for (sg in list) {
                // Adding the element to the container
                c!!.add(sg.node)

                // Adding any triples around it into the model
                if (sg.model != null) m.add(sg.model)
            }
        }
    }

}
