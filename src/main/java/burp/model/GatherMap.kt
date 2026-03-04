package burp.model

import burp.model.gathermap.SubGraph
import org.apache.jena.rdf.model.RDFNode

interface GatherMap {
    fun isGatherMap(): Boolean
    fun generateGatherMapGraphs(i: Iteration, baseIRI: String): List<SubGraph>
    fun generateTerms(i: Iteration, baseIRI: String): List<RDFNode>
}
