package burp.ls

import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import com.opencsv.CSVWriter
import java.io.StringWriter

class CSVIteration(header: Array<String?>?, rec: Array<String?>, nulls: Set<Any?>) : Iteration(nulls) {
    // Use a LinkedHashMap to preserve a correspondence between keys and values
    private val map = mutableMapOf<String?, String?>()

    init {
        if (header != null) {
            for (i in header.indices) {
                map[header[i]] = rec[i]
            }
        }
    }

    override fun getValuesFor(reference: String?, origin: Origin): List<Any?> {
        if (!map.containsKey(reference)) {
            val availableRefs = map.keys.joinToString(", ")
            throw BurpException(
                RmlError(
                    ("Attribute $reference does not exist.\n" +
                            "Available references are: $availableRefs"),
                    origin,
                    RER.ReferenceFormulationExecutionError
                )
            )
        }

        val o = map[reference]

        return if (nulls.contains(o)) emptyList() else listOf(o)
    }

    override fun getStringsFor(reference: String?, origin: Origin): List<String> {
        return getValuesFor(reference, origin).map { obj: Any? -> obj.toString() }.toList()
    }

    override fun asString(): String {
        val stringWriter = StringWriter()
        try {
            CSVWriter(stringWriter).use { writer ->
                val header = map.keys.toTypedArray<String?>()
                writer.writeNext(header)
                val rec = map.values.toTypedArray<String?>()
                writer.writeNext(rec)
            }
        } catch (e: Exception) {
            throw RuntimeException("Error representing CSV iteration as CSV.", e)
        }
        return stringWriter.toString()
    }
}
