package burp.model

import burp.model.gathermap.GatherMap
import burp.reporting.BurpException
import burp.reporting.IncorrectTermType
import burp.vocabularies.BURP
import burp.vocabularies.Rml
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
    var termType: rdf.Term? = null
    
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

    abstract fun getAllowedTermTypes(): Set<rdf.Term>

    override fun generateTerms(i: Iteration): List<Term> {
        val allowed = this.getAllowedTermTypes()

        if (gatherMap != null && allowed.any { it.value == BURP.CollectionOrContainer.uri }) {
            return if (expression == null) {
                gatherMap!!.generateTerms(i, null)
            } else {
                @Suppress("UNCHECKED_CAST") // Cast guaranteed because of disallowed LITERAL
                val generatedIds = generateExpressionTerms(i, setOf(rdfkt.NamedTerm(Rml.Literal))) as List<BlankNodeOrIRI>
                gatherMap!!.generateTerms(i, generatedIds)
            }
        }

        return generateExpressionTerms(i)
    }

    fun generateExpressionTerms(i: Iteration, disallowed: Set<rdf.Term> = emptySet()): List<Term> {
        val allowed = this.getAllowedTermTypes().minus(disallowed)
        val termTypeValue = termType?.value
        val allowedValues = allowed.map { it.value }.toSet()
        return when {
            Rml.IRI == termTypeValue && allowedValues.contains(Rml.IRI) -> generateIRIs(i)
            Rml.URI == termTypeValue && allowedValues.contains(Rml.URI) -> generateURIs(i)
            Rml.UnsafeIRI == termTypeValue && allowedValues.contains(Rml.IRI) -> generateUnsafeIRIs(i)
            Rml.BlankNode == termTypeValue && allowedValues.contains(Rml.BlankNode) -> generateBlankNodes(i)
            Rml.Literal == termTypeValue && allowedValues.contains(Rml.Literal) -> generateLiterals(i, datatypeMap, languageMap)

            else -> throw BurpException(
                IncorrectTermType(
                    this.getName(), termType,
                    this.getAllowedTermTypes(), this
                )
            )
        }
    }
}
