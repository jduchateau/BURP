package burp.model

import burp.reporting.BurpException
import burp.reporting.InvalidRDF
import burp.vocabularies.RER
import org.apache.jena.langtagx.LangTagX
import java.util.function.Consumer

class LanguageMap : ExpressionMap() {
    fun generateStrings(i: Iteration): MutableList<String?> {
        val set: MutableList<String?> = ArrayList<String?>()

        if (expression is RDFNodeConstant) {
            // It is assumed to be a string, otherwise the shapes
            // Would have caught the error.
            set.add((expression as RDFNodeConstant).constant.toString())
        } else if (expression is Template) {
            set.addAll((expression as Template).values(i))
        } else if (expression is Reference) {
            for (o in (expression as Reference).values(i)) set.add(o.toString())
        }

        set.forEach(Consumer { l: String? ->
            if (!isValidLanguageCode(l)) throw BurpException(
                InvalidRDF(
                    "Invalid language code: " + l,
                    expressionOrigin,
                    RER.InvalidLanguageTagError
                )
            )
        })

        return set
    }

    private fun isValidLanguageCode(lang: String?): Boolean {
        return LangTagX.checkLanguageTag(lang)
    }
}