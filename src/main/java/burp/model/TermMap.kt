package burp.model

import burp.model.gathermap.GatherMap
import burp.reporting.BurpException
import burp.reporting.IncorrectTermType
import burp.vocabularies.BURP
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource

abstract class TermMap : ExpressionMap(), TermGenerator {
    var datatypeMap: DatatypeMap? = null
    var languageMap: LanguageMap? = null
    var termType: Resource? = null
    override fun children() =
        sequence {
            yieldAll(super.children())
            if (datatypeMap != null)
                yield(datatypeMap!!)
            if (languageMap != null)
                yield(languageMap!!)
            if (gatherMap != null)
                yield(gatherMap!!)
        }

    var gatherMap: GatherMap? = null

    abstract fun getName(): String

    abstract fun getAllowedTermTypes(): Set<Resource>

    override fun generateTerms(i: Iteration): List<Term> {
        val allowed = this.getAllowedTermTypes()

        if (gatherMap != null && allowed.contains(BURP.CollectionOrContainer)) {
            return if (expression == null) {
                gatherMap!!.generateTerms(i, null)
            } else {
                @Suppress("UNCHECKED_CAST") // Cast guaranteed because of disallowed LITERAL
                val generatedIds = generateExpressionTerms(i, setOf(RML.LITERAL)) as List<BlankNodeOrIRI>
                gatherMap!!.generateTerms(i, generatedIds)
            }
        }

        return generateExpressionTerms(i)
    }

    fun generateExpressionTerms(i: Iteration, disallowed: Set<Resource> = emptySet()): List<Term> {
        val allowed = this.getAllowedTermTypes().minus(disallowed)
        return when {
            RML.IRI == termType && allowed.contains(RML.IRI) -> generateIRIs(i).mapResource()
            RML.URI == termType && allowed.contains(RML.URI) -> generateURIs(i).mapResource()
            RML.UnsafeIRI == termType && allowed.contains(RML.IRI) -> generateUnsafeIRIs(i).mapResource()
            RML.BLANKNODE == termType && allowed.contains(RML.BLANKNODE) -> generateBlankNodes(i)
            RML.LITERAL == termType && allowed.contains(RML.LITERAL) -> generateLiterals(i, datatypeMap, languageMap)

            else -> throw BurpException(
                IncorrectTermType(
                    this.getName(), termType,
                    this.getAllowedTermTypes(), this
                )
            )
        }
    }
}

private fun List<String>.mapResource() = this.map { IRITerm(it) }
