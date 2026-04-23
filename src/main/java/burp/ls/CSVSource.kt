package burp.ls

import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.UnexpectedError
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import org.apache.commons.io.input.BOMInputStream
import org.apache.jena.rdf.model.Resource
import java.io.FileInputStream
import java.io.InputStreamReader

class CSVSource : FileBasedLogicalSource() {
    var delimiter: Char = ','
    var firstLineIsHeader: Boolean = true

    override fun iterator(): Iterator<Iteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf()

                val fileReader = FileInputStream(getDecompressedFile())
                val bomStream = BOMInputStream.builder().setInputStream(fileReader).get()
                val reader = InputStreamReader(bomStream, encoding)

                val csvReader = CSVReaderBuilder(reader)
                    .withCSVParser(
                        CSVParserBuilder()
                            .withSeparator(delimiter)
                            .build()
                    ).build()


                val all = csvReader.readAll()
                csvReader.close()

                // IF THE FIRST LINE IS THE HEADER, REMOVE THE FIRST FROM CSV
                // OTHERWISE, CREATE A LIST OF NUMBERED COLUMNS STARTING FROM ONE
                val header = if (firstLineIsHeader) all.removeAt(0)
                else {
                    val columnCount = all[0]!!.size
                    Array(columnCount) { "${it + 1}" }
                }

                for (rec in all) {
                    iterations!!.add(CSVIteration(header, rec, nulls))
                }
            }
            return iterations!!.iterator()
        } catch (e: BurpException) {
            throw e
        } catch (e: Exception) {
            throw BurpException(UnexpectedError(e, this@CSVSource))
        }
    }

    override var referenceFormulation: Resource
        get() = RML.CSV
        set(value) {}

    override fun buildExportedReference(reference: String, origin: Origin) = CSVReference(reference, origin)
}

class CSVReference(reference: String?, origin: burp.reporting.Origin) : burp.model.Reference(reference, origin) {
    override fun getValues(i: Iteration): List<Any?> {
        require(i is CSVIteration) { "CSVReference $reference can only be used with CSVIteration."}
        if (!i.map.containsKey(reference)) {
            val availableRefs = i.map.keys.joinToString(", ")
            throw BurpException(
                burp.reporting.RmlError(
                    ("Attribute $reference does not exist.\n" +
                            "Available references are: $availableRefs"),
                    origin,
                    RER.ReferenceFormulationExecutionError
                )
            )
        }

        val o = i.map[reference]

        return if (i.nulls.contains(o)) emptyList() else listOf(o)
    }
}

