package burp.ls

import at.asitplus.jsonpath.JsonPath
import at.asitplus.jsonpath.core.JsonPathCompilerException
import at.asitplus.jsonpath.core.JsonPathQueryException
import at.asitplus.jsonpath.core.NodeListEntry
import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.google.auto.service.AutoService
import kotlinx.serialization.json.*
import org.apache.jena.rdf.model.Resource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
public class JSONSourceProvider : LogicalSourceProvider {

    override fun supports(referenceFormulation: Resource): Boolean =
        RML.JSONPath == referenceFormulation

    override fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val source = ls.getPropertyResourceValue(RML.source)
        val iterator = ls.getProperty(RML.iterator).literal.string

        val (sourceFile, origin) = getFile(source, mappingDirectory, currentWorkingDirectory)

        return JSONSourceRFC().apply {
            this.file = sourceFile
            this.fileOriginStmts = origin
            this.iterator = iterator
            this.encoding = getEncoding(source)
            this.compression = getCompression(source)
            this.nulls.addAll(getNullValues(source))
            this.referenceFormulation = RML.JSONPath
        }
    }
}


private class JSONSourceRFC : FileBasedLogicalSource() {
    override fun iterator(): Iterator<JSONIterationRFC> {
        try {
            val contents = Files.readString(Paths.get(getDecompressedFile()), encoding)
            val jsonContent = Json.parseToJsonElement(contents)
            val results = JsonPath(iterator).query(jsonContent)
            return results.map { JSONIterationRFC(it, nulls) }.iterator()
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }
}

class JSONIterationRFC(val json: NodeListEntry, nulls: Set<Any>) : Iteration(nulls) {

    override fun getValuesFor(reference: String?): List<Any?> {
        // We need to explicitly convert the objects
        // to strings because RML has not worked out
        // "6.6.1 Automatically deriving datatypes" yet
        val resultList: MutableList<Any?> = mutableListOf()
        try {
            val entries = JsonPath(reference ?: "").query(json.value)
            for (entry in entries) {
                when (val jsonElement = entry.value) {
                    is JsonArray -> throw RuntimeException("Data error: reference retrieved an array")
                    is JsonObject -> resultList.add(jsonElement.toString())
                    is JsonNull -> /* ignore nulls: https://kg-construct.github.io/rml-io/spec/docs/#null-values*/ {}
                    is JsonPrimitive -> {
                        val content =
                            if (jsonElement.isString) jsonElement.content
                            else
                                jsonElement.intOrNull
                                    ?: jsonElement.longOrNull
                                    ?: jsonElement.floatOrNull
                                    ?: jsonElement.doubleOrNull
                                    ?: jsonElement.booleanOrNull
                        if (content !in nulls) resultList.add(content)
                    }
                }
            }
        } catch (ex: Exception) {
            when (ex) {
                is JsonPathCompilerException -> throw BurpException(
                    RmlError(
                        "Syntax error in JSONPath",
                        Origin(this, null), //TODO
                        RER.ReferenceFormulationSyntaxError,
                        ex
                    )
                )

                is JsonPathQueryException -> throw BurpException(
                    RmlError(
                        "Execution error in JSONPath",
                        Origin(this, null),
                        RER.ReferenceFormulationExecutionError,
                        ex
                    )
                )

                else -> throw BurpException(RmlError.UnexpectedError(ex, this))
            }
        }
        return resultList
    }

    override fun getStringsFor(reference: String?): List<String?> =
        getValuesFor(reference).map { it.toString() }.toList()

    override fun asString(): String {
        return json.value.toString()
    }


}