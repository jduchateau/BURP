package burp.model

import burp.model.gathermap.GatherMapMixin
import burp.model.gathermap.SubGraph
import burp.reporting.BurpException
import burp.reporting.IncorrectTermType
import burp.reporting.PlanNode
import burp.vocabularies.RML
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import kotlin.String
import kotlin.check
import kotlin.collections.Iterable
import kotlin.collections.map

abstract class TermMap : ExpressionMap(), GatherMap, PlanNode {
    var datatypeMap: DatatypeMap? = null
    var languageMap: LanguageMap? = null
    var termType: Resource? = null

    var gatherMap: GatherMapMixin? = null

    override fun generateGatherMapGraphs(i: Iteration, baseIRI: String): List<SubGraph> {
        check(isGatherMap()) { "Trying to process a non-gathermap as gathermap" }

        val g = mutableListOf<SubGraph>()

        if (expression == null) {
            g.addAll(gatherMap!!.generateGraphs(i, baseIRI))
        } else {
            for (n in generateTerms(i, baseIRI)) {
                for (sg in gatherMap!!.generateGraphs(i, baseIRI)) {
                    sg.updateNode(n)
                    g.add(sg)
                }
            }
        }

        return g
    }


    abstract fun getName(): String

    abstract fun getAllowedTermTypes(): List<Resource>

    override fun generateTerms(i: Iteration, baseIRI: String): List<RDFNode> {
        val allowed = this.getAllowedTermTypes()

        return when {
            RML.IRI == termType && allowed.contains(RML.IRI) -> generateIRIs(i, baseIRI).mapResource()
            RML.URI == termType && allowed.contains(RML.URI) -> generateURIs(i, baseIRI).mapResource()
            RML.UnsafeIRI == termType && allowed.contains(RML.IRI) -> generateUnsafeIRIs(i, baseIRI).mapResource()
            RML.UnsafeURI == termType && allowed.contains(RML.URI) -> generateUnsafeURIs(i, baseIRI).mapResource()
            RML.BLANKNODE == termType && allowed.contains(RML.BLANKNODE) -> generateBlankNodes(i, baseIRI)
            RML.LITERAL == termType && allowed.contains(RML.LITERAL) ->
                generateLiterals(i, baseIRI, datatypeMap, languageMap)

            else -> throw BurpException(
                IncorrectTermType(
                    this.getName(), termType!!,
                    this.getAllowedTermTypes(), this
                )
            )
        }
    }
}

private fun Iterable<String>.mapResource(): List<Resource> = this.map { ResourceFactory.createResource(it) }