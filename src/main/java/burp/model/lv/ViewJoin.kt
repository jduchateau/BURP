package burp.model.lv

import burp.Main
import burp.model.JoinCondition
import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER

class ViewJoin {
    lateinit var parentLogicalView: LogicalView
    var joinConditions = mutableListOf<JoinCondition>()
    var expressionFields = mutableListOf<ExpressionField>()
    var isInnerJoin: Boolean = false

    private var iterations: List<LogicalIteration>? = null

    fun expand(childIterations: MutableList<LogicalIteration>): MutableList<LogicalIteration> {
        try {
            if (iterations == null) {
                // Get logical iterations of the parent logical view
                iterations = parentLogicalView.iterator()
                    .asSequence()
                    .filterIsInstance<LogicalIteration>()
                    .toList()
            }

            val newList = mutableListOf<LogicalIteration>()

            for (childIteration in childIterations) {
                var hasACorrespondence = false

                // Discussion with Els. For each child, the counter for the parents are "reset" to 0
                // So we assign 0, 1, ..., n from the child's perspective.
                var count = -1 // start at -1 as no match found yet

                for (parentIteration in iterations!!) {
                    if (matches(childIteration, parentIteration)) {
                        hasACorrespondence = true

                        // Increment the index
                        count++

                        // Create new logical iterations for each value of each expression map
                        var result: MutableList<LogicalIteration> = ArrayList<LogicalIteration>()
                        result.add(childIteration)
                        for (e in expressionFields) {
                            result = enrichForJoin(e, count, result, parentIteration)
                        }

                        // add all new iterations to the list
                        newList.addAll(result)
                    }
                }

                // Make the outer join if there is no match
                if (!hasACorrespondence && !isInnerJoin) {
                    val newIteration = childIteration.copy()
                    for (e in expressionFields) {
                        newIteration.put(e.fieldName, null)
                        newIteration.put(e.fieldName + ".#", null)
                    }
                    newList.add(newIteration)
                }
            }

            return newList
        } catch (e: BurpException) {
            throw e // rethrow
        } catch (e: Throwable) {
            throw BurpException(RmlError("Error while expanding ViewJoin: ${e.message}", null, RER.ExecutionError))
        }
    }

    private fun enrichForJoin(
        e: ExpressionField,
        index: Int,
        result: MutableList<LogicalIteration>,
        parentIteration: LogicalIteration
    ): MutableList<LogicalIteration> {
        val nList = mutableListOf<LogicalIteration>()

        for (li in result) {
            for (o in e.fieldExpressionMap.generateValues(parentIteration, Main.conf.baseIRI)) {
                val newLogicalIteration = li.copy()
                newLogicalIteration.put(e.fieldName, o)
                newLogicalIteration.put(e.fieldName + ".#", index)
                nList.add(newLogicalIteration)
            }
        }

        return nList
    }

    private fun matches(childIteration: LogicalIteration, parentIteration: LogicalIteration): Boolean {
        // Expression Maps are multi-valued. We thus need
        // For each join condition at least one match.
        return joinConditions.all { jc ->
            val values1 = jc.childMap.generateValues(childIteration, Main.conf.baseIRI).toSet()
            val values2 = jc.parentMap.generateValues(parentIteration, Main.conf.baseIRI).toSet()
            values1.any { it in values2 }
        }
    }

    fun addField(field: Field) {
        field.parent = parentLogicalView
        if (field is ExpressionField) {
            this.expressionFields.add(field)
        } else throw RuntimeException("Unknown field type for ViewJoin.")
    }
}
