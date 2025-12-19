package burp.model

import burp.reporting.BurpException
import burp.reporting.Origin
import com.opencsv.CSVWriter
import java.io.StringWriter
import kotlin.math.max

class LogicalView : AbstractLogicalSource(), ContainsFields {
    private var iterations: MutableList<LogicalIteration>? = null

    var logicalSource: AbstractLogicalSource? = null

    override var expressionFields = mutableListOf<ExpressionField>()
    override var iterableFields = mutableListOf<IterableField>()

    var joins = mutableListOf<ViewJoin>()

    @Throws(BurpException::class)
    override fun iterator(): Iterator<Iteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf()

                val list = mutableListOf<LogicalIteration>()
                val iterator = logicalSource!!.iterator()
                var index = 0
                while (iterator.hasNext()) {
                    val i = iterator.next()
                    val li = LogicalIteration(logicalSource!!.nulls)
                    li.put("#", index++)
                    li.put("<i>", i)
                    list.add(li)
                }

                iterations = Field.expand(list, expressionFields, iterableFields)

                for (join in joins) {
                    iterations = join.expand(iterations)
                }
            }
            return iterations!!.iterator()
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }


    override fun addField(field: Field) {
        // The parent of a logical view's fields is its logical source.
        field.parent = this.logicalSource

        when (field) {
            is IterableField -> iterableFields.add(field)
            is ExpressionField -> expressionFields.add(field)
            else -> throw RuntimeException("Unknown field type.")
        }
    }

    fun addJoin(join: ViewJoin?) {
        joins.add(join!!)
    }
}

internal class LogicalIteration(
    private var map: MutableMap<String, Any?>,
    nulls: Set<Any?>
) : Iteration(nulls) {

    constructor(nulls: Set<Any?>) : this(mutableMapOf(), nulls)

    override fun getValuesFor(reference: String?, origin: Origin): List<Any?> {
        val l: MutableList<Any?> = ArrayList<Any?>()
        if (!map.containsKey(reference)) throw RuntimeException("Attribute $reference does not exist.")

        val o = map[reference]

        if (o is Iteration) throw RuntimeException("Attribute $reference refers to a record key.")

        if (!nulls.contains(o)) l.add(o)

        return l
    }

    override fun getStringsFor(reference: String?, origin: Origin): MutableList<String> {
        return getValuesFor(reference, origin)
            .mapNotNull { it?.toString() }
            .toMutableList()
    }

    override fun asString(): String {
        val stringWriter = StringWriter()
        try {
            CSVWriter(stringWriter).use { writer ->
                val header = map.keys.toTypedArray<String>()
                writer.writeNext(header)
                val rec = map.values.map { it.toString() }.toTypedArray<String>()
                writer.writeNext(rec)
            }
        } catch (e: Exception) {
            throw RuntimeException("Error representing logical iteration as String/CSV.")
        }
        return stringWriter.toString()
    }

    override fun toString(): String {
        val widths: MutableMap<String?, Int?> = LinkedHashMap<String?, Int?>()
        for (e in map.entries) {
            val width = max(e.key.length, e.value.toString().length)
            widths[e.key] = width
        }

        val sb = StringBuilder()

        // Build horizontal line
        val line =
            widths.values.joinToString(separator = "+", prefix = "+", postfix = "+") { w -> "-".repeat((w ?: 0) + 2) }

        // Header row (keys)
        sb.append(line).append("\n")
        sb.append("|")
        for (e in map.entries) {
            sb.append(" ").append(String.format("%-" + widths.get(e.key) + "s", e.key)).append(" |")
        }
        sb.append("\n").append(line).append("\n")

        // Value row
        sb.append("|")
        for (e in map.entries) {
            sb.append(" ").append(String.format("%-" + widths.get(e.key) + "s", e.value)).append(" |")
        }
        sb.append("\n").append(line)

        return sb.toString()
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
    fun getIteration(fieldName: String?): Iteration? {
        return map[fieldName] as Iteration?
    }

    // Used by IterableField
    fun getIterationString(fieldName: String?): String? {
        val o: Any = map[fieldName]!!
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