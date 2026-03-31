package burp.model.lv

import burp.model.AbstractLogicalSource
import burp.model.PlanNode

abstract class Field : ContainsFields, FieldParent, PlanNode {
    lateinit var fieldName: String
    lateinit var parentField: FieldParent

    override var expressionFields = mutableListOf<ExpressionField>()
    override var iterableFields = mutableListOf<IterableField>()

    override var parent: PlanNode? = null

    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(expressionFields)
        yieldAll(iterableFields)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    override val absoluteFieldName: String
        get() {
            if (parentField is AbstractLogicalSource) return fieldName

            val parentF = this.parentField as Field
            return parentF.absoluteFieldName + "." + fieldName
        }

    override fun addField(field: Field) {
        field.parentField = this

        when (field) {
            is IterableField -> iterableFields.add(field)
            is ExpressionField -> expressionFields.add(field)
            else -> throw RuntimeException("Unknown field type.")
        }
    }

    companion object {
        fun expand(
            iterations: List<LogicalIteration>,
            expressionFields: List<ExpressionField>?,
            iterableFields: List<IterableField>?
        ): MutableList<LogicalIteration> {
            var result = ArrayList<LogicalIteration>(iterations) as MutableList<LogicalIteration>

            if (!expressionFields.isNullOrEmpty()) {
                // Let's process the expression fields
                for (expressionField in expressionFields) {
                    val newIterations = mutableListOf<LogicalIteration>()
                    for (li in result) {
                        newIterations.addAll(expressionField.enrich(li))
                    }
                    result = newIterations
                }
            }

            if (!iterableFields.isNullOrEmpty()) {
                // Let's process the iterable fields
                val newIterations = mutableListOf<LogicalIteration>()
                for (li in result) {
                    for (iterableField in iterableFields) {
                        newIterations.addAll(iterableField.enrich(li))
                    }
                }
                result = newIterations
            }

            return result
        }
    }
}

