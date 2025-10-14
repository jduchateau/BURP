package burp.model

import burp.errors.BurpException
import burp.vocabularies.RMLError
import java.util.regex.Pattern

class LanguageMap(expression: Expression?) : ExpressionMap(expression) {
    @Throws(BurpException::class)
    fun generateStrings(i: Iteration, baseIRI: String?): Set<String> {
        val languageTags = mutableSetOf<String?>()

        when (expression) {
            is RDFNodeConstant -> {
                // It is assumed to be a string, otherwise the shapes
                // Would have caught the error.
                languageTags.add(expression.constant.toString())
            }

            is Template -> {
                languageTags.addAll(expression.values(i))
            }

            is Reference -> {
                languageTags.addAll(expression.values(i).map { it.toString() })
            }

            is FunctionExecution -> {
                languageTags.addAll(expression.values(i, baseIRI).map { it.toString() })
            }

            else -> throw BurpException(RMLError.SpecUnsupported, "Unexpected expression in LanguageMap.", this)
        }

        val languageTagsWithoutNull = languageTags.filterNotNull()
        languageTagsWithoutNull.forEach {
            if (!isValidLanguageCode(it)) throw BurpException(RMLError.OutOfSpec, "Invalid language code: $it", this)
        }

        return languageTagsWithoutNull.toSet()
    }

    private fun isValidLanguageCode(lang: String) = p.matcher(lang).find()

    companion object {
        // Source of REGEX based on, but reduced: https://www.regextester.com/103066
        private val p: Pattern =
            Pattern.compile("^(((?:([A-Za-z]{2,3}(-(?:[A-Za-z]{3}(-[A-Za-z]{3}){0,2}))?))(-(?:[A-Za-z]{4}))?(-(?:[A-Za-z]{2}|[0-9]{3}))?(-(?:[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*(-(?:[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+))*(-(?:x(-[A-Za-z0-9]{1,8})+))?)|(?:x(-[A-Za-z0-9]{1,8})+))$")
    }
}