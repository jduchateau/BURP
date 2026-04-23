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
    fun generateLanguageTags(i: Iteration): List<String> {
        return generateValues(i, Unsafe)
            .filterNotNull()
            .map {
                val string = when (it) {
                    is LiteralTerm -> it.value
                    else -> it.toString()
                }
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