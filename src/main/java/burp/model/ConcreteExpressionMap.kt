package burp.model

import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER

/**
 * 
 * A ConcreteExpressionMap is a concrete implementation of the abstract class
 * ExpressionMap for use in join conditions and logical views
 * 
 */
class ConcreteExpressionMap : ExpressionMap() {
    fun generateValues(i: Iteration, baseIRI: String): List<Any?> =
        when (val expr = expression) {
            is RDFNodeConstant -> mutableListOf<Any?>(expr.constant.toString())
            is Template -> ArrayList<Any?>(expr.values(i))
            is Reference -> expr.values(i)
            is FunctionExecution -> expr.values(i, baseIRI)
            else -> throw BurpException(
                RmlError(
                    "Unsupported type of values expression in Expression Map.",
                    expressionOrigin,
                    RER.UnsupportedMapping
                )
            )
        }
}