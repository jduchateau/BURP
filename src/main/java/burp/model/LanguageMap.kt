package burp.model

import burp.model.TemplateReferenceSafety.Unsafe
import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER
import org.apache.jena.langtagx.LangTagX

class LanguageMap : ExpressionMap() {

    /**
     * Generate valid Language Tags according to RFC 5646
     */
    fun generateLanguageTags(i: Iteration, baseIRI: String): List<String> {
        return generateValues(i, baseIRI, Unsafe)
            .filterNotNull()
            .map {
                val string = it.toString()
                if (LangTagX.checkLanguageTag(string))
                    return listOf(string)
                else
                    throw BurpException(
                        RmlError(
                            "Invalid language code: $it",
                            expressionOrigin,
                            RER.InvalidLanguageTagError
                        )
                    )
            }
    }
}