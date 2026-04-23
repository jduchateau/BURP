package burp.model

import burp.model.fnmlutil.FunctionsRegistry
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.reporting.StatementParts
import burp.vocabularies.RER

class FunctionExecution() : Expression {
    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        if (functionMap != null) yield(functionMap!!)
        yieldAll(inputs.flatMap { listOf(it.parameterMap, it.inputValueMap).filterNotNull() })
        if (returnMap != null) yield(returnMap!!)
    }
    override fun dependencies(): Sequence<PlanNode> = emptySequence()

    override fun nodeRanges(): List<burp.reporting.PointRange> {
        val pointers = mutableListOf<burp.reporting.RDFGraphPointer>()
        pointers.add(callStmt)
        functionMapStmt?.let { pointers.add(it) }
        returnMapStmt?.let { pointers.add(it) }
        pointers.addAll(inputsStmt)
        return turtleprov.retrieveTurtleLocation(pointers)
    }

    var functionMap: FunctionMap? = null
    var inputs: MutableList<Input> = ArrayList<Input>()
    var returnMap: ReturnMap? = null

    lateinit var callStmt: StatementParts
    var inputsStmt: List<StatementParts> = listOf()
    var returnMapStmt: StatementParts? = null
    var functionMapStmt: StatementParts? = null

    fun values(iteration: Iteration): List<Any?> {
        val list = mutableListOf<Any?>()

        // TODO: We assume that function maps, parameter maps, and input value maps only yield one value
        val functions = functionMap!!.generateIRIs(iteration)
        if (functions.size != 1) throw BurpException(
            RmlError(
                "Function map should generate exactly one value.",
                Origin(this, listOfNotNull(functionMapStmt)),
                RER.FunctionExecutionError
            )
        )

        val function = functions[0]

        // Bind parameters via a map
        val map = mutableMapOf<String, Any?>()

        for ((index, input) in inputs.withIndex()) {
            val parameters = input.parameterMap.generateIRIs(iteration)
            if (parameters.size != 1) throw BurpException(
                RmlError(
                    "Parameter map should generate exactly one value.",
                    Origin(this, listOf(inputsStmt[index])),
                    RER.FunctionExecutionError
                )
            )

            val parameter = parameters[0]

            val inputs = input.inputValueMap.generateTerms(iteration)
            if (inputs.size != 1) throw BurpException(
                RmlError(
                    "Input value map should generate exactly one value.",
                    Origin(this, listOf(inputsStmt[index])),
                    RER.FunctionExecutionError
                )
            )

            map[parameter] = inputs.first()
        }

        val originCall = Origin(this, listOf(callStmt))

        for (o in FunctionsRegistry.execute(function, map, originCall)) {
            // if return map is null, then we return the default return value
            // Otherwise, look for the value identified by the return map
            val originReturnMap = Origin(this, listOfNotNull(returnMapStmt))
            if (returnMap == null) {
                list.add(o.defaultValue)
            } else {
                val returns = returnMap!!.generateIRIs(iteration)
                if (returns.size != 1) {
                    throw BurpException(
                        RmlError(
                            "Input value map should generate exactly one value.",
                            originReturnMap,
                            RER.FunctionExecutionError
                        )
                    )
                }

                val v = o.get(returns[0], originReturnMap)
                list.add(v)
            }
        }

        return list
    }
}