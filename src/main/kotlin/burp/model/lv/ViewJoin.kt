package burp.model.lv

import burp.model.*
import burp.model.TemplateReferenceSafety.SafeIRI
import burp.model.TemplateReferenceSafety.Unsafe
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER

enum class JoinType {
    INNER, LEFT
}

class ViewJoin : PlanNode, ParentJoinReferenceScope, LocalReferenceScope, ReferenceHolder {
    lateinit var parentLogicalView: LogicalView
    var joinConditions = mutableListOf<JoinCondition>()
    var expressionFields = mutableListOf<ExpressionField>()
    lateinit var joinType: JoinType

    private var iterations: List<LogicalIteration>? = null

    override var parent: PlanNode? = null

    override fun children(): Sequence<PlanNode> = sequence {
        yield(parentLogicalView)
        yieldAll(joinConditions)
        yieldAll(expressionFields)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

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
                if (!hasACorrespondence && joinType == JoinType.LEFT) {
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
            for (o in e.fieldExpressionMap.generateValues(parentIteration, SafeIRI)) {
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
            val values1 = jc.childMap.generateValues(childIteration, Unsafe).toSet()
            val values2 = jc.parentMap.generateValues(parentIteration, Unsafe).toSet()
            values1.any { v1 -> values2.any { v2 -> valuesMatch(v1, v2) } }
        }
    }

    fun addField(field: Field) {
        field.parentField = parentLogicalView
        if (field is ExpressionField) {
            this.expressionFields.add(field)
        } else throw RuntimeException("Unknown field type for ViewJoin.")
    }


    override fun buildLocalReference(reference: String, origin: Origin): Reference {
        return (parent as LogicalView).buildExportedReference(reference, origin)
    }

    override fun buildParentJoinReference(reference: String, origin: burp.reporting.Origin): burp.model.Reference {
        return parentLogicalView.buildExportedReference(reference, origin)
    }

    override fun compileReferences() {
        // The expressionFields map data from the parent LogicalView into the current iteration.
        // Therefore, they must explicitly compile using the ParentJoinReferenceScope.
        for (field in expressionFields) {
            for (ref in field.descendants<RawReference>()) {
                if (ref.reference != null && ref.compiledReference == null) {
                    ref.compiledReference = this.buildParentJoinReference(ref.reference, ref.origin)
                }
            }
        }
    }
}
