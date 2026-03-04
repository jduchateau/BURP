package burp.model.lv

import burp.Main
import burp.model.ConcreteExpressionMap
import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER

class ExpressionField : Field() {
    lateinit var fieldExpressionMap: ConcreteExpressionMap

    fun enrich(underlying: LogicalIteration): MutableList<LogicalIteration> {
        val underlyingIteration = underlying.getIteration(parent.absoluteFieldName)
        if (underlyingIteration == null) {
            throw BurpException(
                RmlError(
                    "Cannot get iterations for ${parent.absoluteFieldName}, which is required for expression field $absoluteFieldName.",
                    null, // TODO: Add origin to field
                    RER.ReferenceFormulationExecutionError
                )
            )
        }
        val list = fieldExpressionMap
            .generateValues(underlyingIteration, Main.conf.baseIRI)
            .mapIndexed { i, o ->
                val e = underlying.copy()
                e.put("$absoluteFieldName.#", i)
                e.put(absoluteFieldName, o)
                e
            }

        return expand(list, expressionFields, iterableFields)
    }
}
