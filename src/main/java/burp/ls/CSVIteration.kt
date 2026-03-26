package burp.ls

import burp.model.Iteration
import com.opencsv.CSVWriter
import java.io.StringWriter

class CSVIteration(header: Array<String?>?, rec: Array<String?>, nulls: Set<Any?>) : Iteration(nulls) {
    // Use a LinkedHashMap to preserve a correspondence between keys and values
    val map = mutableMapOf<String?, String?>()

    init {
        if (header != null) {
            for (i in header.indices) {
                map[header[i]] = rec[i]
            }
        }
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
