package burp.model.lv

import burp.model.ConcreteExpressionMap

class ExpressionField : Field() {
    lateinit var fieldExpressionMap: ConcreteExpressionMap

    fun enrich(underlying: LogicalIteration): MutableList<LogicalIteration> {
        val list = fieldExpressionMap
            .generateValues(underlying.getIteration(parent.absoluteFieldName))
            .mapIndexed { i, o ->
                val e = underlying.copy()
                e.put("$absoluteFieldName.#", i)
                e.put(absoluteFieldName, o)
                e
            }

        return expand(list, expressionFields, iterableFields)
    }
}
