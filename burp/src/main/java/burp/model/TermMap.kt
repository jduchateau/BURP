package burp.model

import burp.model.gathermap.GatherMap
import burp.reporting.BurpException
import burp.reporting.IncorrectTermType
import burp.vocabularies.BURP
import burp.vocabularies.RML
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfShortcutProperty

abstract class TermMap : ExpressionMap(), TermGenerator {
    @RdfProperty(Rml.datatypeMap)
    @RdfShortcutProperty(Rml.datatype, Rml.constant)
    var datatypeMap: DatatypeMap? = null

    @RdfProperty(Rml.languageMap)
    @RdfShortcutProperty(Rml.language, Rml.constant)
    var languageMap: LanguageMap? = null

    @RdfProperty(Rml.termType)
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

    @RdfProperty(Rml.gather)
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
            RML.IRI == termType && allowed.contains(RML.IRI) -> generateIRIs(i)
            RML.URI == termType && allowed.contains(RML.URI) -> generateURIs(i)
            RML.UnsafeIRI == termType && allowed.contains(RML.IRI) -> generateUnsafeIRIs(i)
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
