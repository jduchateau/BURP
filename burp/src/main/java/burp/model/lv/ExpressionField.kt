package burp.model.lv

import burp.model.ConcreteExpressionMap
import burp.model.PlanNode
import burp.model.TemplateReferenceSafety.SafeIRI
import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER

class ExpressionField : Field() {
    lateinit var fieldExpressionMap: ConcreteExpressionMap

    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(super.children())
        yield(fieldExpressionMap)
    }

    fun enrich(underlying: LogicalIteration): MutableList<LogicalIteration> {
        val underlyingIteration = underlying.getIteration(parentField.absoluteFieldName)
            ?: throw BurpException(
                RmlError(
                    "Cannot get iterations for ${parentField.absoluteFieldName}, which is required for expression field $absoluteFieldName.",
                    null, // TODO: Add origin to field
                    RER.ReferenceFormulationExecutionError
                )
            )

        val generatedValues = fieldExpressionMap.generateValues(underlyingIteration, SafeIRI)
        val list = if (generatedValues.isEmpty()) {
            //FIXME: How should we register that a field with no result ?
            // So that later on they can request that field an receive nothing (example with RMLLVTC0010b)
            underlying.put("$absoluteFieldName.#", null)
            underlying.put(absoluteFieldName, null)
            listOf(underlying)
        } else {
            generatedValues.mapIndexed { i, o ->
                val e = underlying.copy()
                e.put("$absoluteFieldName.#", i)
                e.put(absoluteFieldName, o)
                e
            }
        }

        return expand(list, expressionFields, iterableFields)
    }
}
