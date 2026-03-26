package burp.model.lv

import burp.model.AbstractLogicalSource
import burp.model.Iteration
import burp.model.Reference
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.BURP
import burp.vocabularies.RER
import com.opencsv.CSVWriter
import org.apache.jena.rdf.model.Resource
import java.io.StringWriter

class LogicalView : AbstractLogicalSource(), ContainsFields {
    private var iterations: MutableList<LogicalIteration>? = null

    lateinit var logicalSource: AbstractLogicalSource

    override var expressionFields = mutableListOf<ExpressionField>()
    override var iterableFields = mutableListOf<IterableField>()

    var joins = mutableListOf<ViewJoin>()

    @Throws(BurpException::class)
    override fun iterator(): Iterator<Iteration> {
        if (iterations == null) {
            iterations = mutableListOf()

            val viewOnIterations = logicalSource.iterator().asSequence()
                .mapIndexed { index, iteration ->
                    val li = LogicalIteration(logicalSource.nulls)
                    li.put("#", index)
                    li.put("<i>", iteration)
                    li
                }.toList()

            iterations = Field.expand(viewOnIterations, expressionFields, iterableFields)

            for (join in joins) {
                iterations = join.expand(iterations!!)
            }
        }
        return iterations!!.iterator()
    }


    override fun addField(field: Field) {
        // The parent of a logical view's fields is its logical source.
        field.parentField = this.logicalSource

        when (field) {
            is IterableField -> iterableFields.add(field)
            is ExpressionField -> expressionFields.add(field)
            else -> throw RuntimeException("Unknown field type.")
        }
    }

    fun addJoin(join: ViewJoin) {
        joins.add(join)
    }

    override var referenceFormulation: Resource
        get() = BURP.LogicalView
        set(value) {}

    override fun buildReference(reference: String, origin: Origin): burp.model.Reference {
        return LogicalReference(reference, origin)
    }
}

class LogicalReference(reference: String, origin: Origin) : Reference(reference, origin) {
    override fun getValues(i: Iteration): List<Any?> {
        require(i is LogicalIteration)

        if (!i.map.containsKey(reference)) throw BurpException(
            RmlError(
                "Attribute $reference does not exist.",
                origin,
                errorType = RER.ReferenceFormulationExecutionError,
                context = mapOf(RER.reference to reference)
            )
        )

        val o = i.map[reference]

        if (o is Iteration) throw BurpException(
            RmlError(
                "Attribute $reference refers to a record key.", origin,
                errorType = RER.ReferenceFormulationExecutionError,
                context = mapOf(RER.reference to reference)
            )
        )

        if (i.nulls.contains(o)) return emptyList()
        return listOf(o)
    }
}

class LogicalIteration(internal var map: MutableMap<String, Any?>, nulls: Set<Any?>) : Iteration(nulls) {

    constructor(nulls: Set<Any?>) : this(mutableMapOf(), nulls)

    override fun asString(): String {
        val stringWriter = StringWriter()
        try {
            CSVWriter(stringWriter).use { writer ->
                val header = map.keys.toTypedArray<String>()
                writer.writeNext(header)
                val rec = map.values.map { it.toString() }.toTypedArray<String>()
                writer.writeNext(rec)
            }
        } catch (_: Exception) {
            throw RuntimeException("Error representing logical iteration as String/CSV.")
        }
        return stringWriter.toString()
    }

    fun copy(): LogicalIteration {
        return LogicalIteration(map.toMutableMap(), nulls)
    }

    fun put(key: String, o: Any?) {
        if (map.containsKey(key)) {
            throw RuntimeException("Attribute $key already exists in logical iteration (duplicate names in fields or joins).")
        }
        map[key] = o
    }

    // Used by ExpressionField
    fun getIteration(fieldName: String): Iteration? {
        return map[fieldName] as? Iteration?
    }

    // Used by IterableField
    fun getIterationString(fieldName: String): String? {
        val o = map[fieldName]
        if (o is Iteration) return o.asString()
        return o.toString()
    }

    fun retainKeys(keys: MutableList<String?>) {
        // remove all the keys in the map.
        map.keys.retainAll(keys.toSet())
    }

    fun add(iteration: LogicalIteration) {
        for (k in iteration.map.keys) {
            // Use the Iteration's put instead of the maps to ensure not duplicate fields.
            put(k, iteration.map[k])
        }
    }
}