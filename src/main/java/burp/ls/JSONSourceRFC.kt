package burp.ls

import at.asitplus.jsonpath.JsonPath
import at.asitplus.jsonpath.core.*
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompiler
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompilerErrorListener
import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.BurpException
import burp.reporting.LiteralPart
import burp.reporting.Origin
import burp.reporting.PointRange
import burp.reporting.RmlError
import burp.vocabularies.RML
import burp.vocabularies.RER
import com.google.auto.service.AutoService
import kotlinx.serialization.json.*
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.apache.jena.rdf.model.Resource
import turtleprov.Point
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
public class JSONSourceProvider : LogicalSourceProvider {

    override fun supports(referenceFormulation: Resource): Boolean = RML.JSONPath == referenceFormulation

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
        val contents = Files.readString(Paths.get(getDecompressedFile()), encoding)
        val jsonContent = Json.parseToJsonElement(contents)
        val results = JsonPath(
            iterator, AntlrJsonPathCompiler(errorListener = capturingAntlrJsonPathCompilerErrorListener())
        ).query(jsonContent)
        return results.map { JSONIterationRFC(it, nulls) }.iterator()
    }
}

class JSONIterationRFC(val json: NodeListEntry, nulls: Set<Any>) : Iteration(nulls) {

    override fun getValuesFor(reference: String, origin: Origin): List<Any?> {
        // We need to explicitly convert the objects
        // to strings because RML has not worked out
        // "6.6.1 Automatically deriving datatypes" yet
        val resultList: MutableList<Any?> = mutableListOf()
        val antlrErrorListener = capturingAntlrJsonPathCompilerErrorListener()

        try {
            val entries = JsonPath(
                reference ?: "", AntlrJsonPathCompiler(errorListener = antlrErrorListener)
            ).query(json.value)
            for (entry in entries) {
                when (val jsonElement = entry.value) {
                    is JsonArray -> throw BurpException(
                        RmlError(
                            "Data error: reference retrieved an array with `$reference`",
                            origin,
                            RER.ReferenceFormulationExecutionError,
                        )
                    )

                    is JsonObject -> resultList.add(jsonElement.toString())
                    is JsonNull -> /* ignore nulls: https://kg-construct.github.io/rml-io/spec/docs/#null-values*/ {}
                    is JsonPrimitive -> {
                        val content = if (jsonElement.isString) jsonElement.content
                        else jsonElement.intOrNull ?: jsonElement.longOrNull ?: jsonElement.floatOrNull
                        ?: jsonElement.doubleOrNull ?: jsonElement.booleanOrNull
                        if (content !in nulls) resultList.add(content)
                    }
                }
            }
        } catch (ex: Exception) {
            when (ex) {
                is JsonPathCompilerException if antlrErrorListener.antlrErrors.isEmpty() -> throw BurpException(
                    RmlError(
                        "Syntax error in JSONPath `$reference`", origin, RER.ReferenceFormulationSyntaxError, ex
                    )
                )

                is JsonPathCompilerException if antlrErrorListener.antlrErrors.isNotEmpty() -> {
                    // TODO Be able to report more than one
                    val antlrError = antlrErrorListener.antlrErrors.first()
                    throw BurpException(
                        RmlError.ReferenceFormulationSyntaxError(
                            "Syntax error in JSONPath `$reference` at ${antlrError.start.line}:${antlrError.start.column}: ${antlrError.msg}",
                            origin.copy(
                                sourceStatements = listOf(
                                    LiteralPart(
                                        origin.sourceStatements!!.first().stmt,
                                        PointRange(antlrError.start)
                                    )
                                )
                            )
                        )
                    )
                }


                is JsonPathQueryException -> throw BurpException(
                    RmlError(
                        "Execution error in JSONPath `$reference`", origin, RER.ReferenceFormulationExecutionError, ex
                    )
                )

                is BurpException -> throw ex

                else -> throw BurpException(RmlError.UnexpectedError(ex, origin))
            }
        }
        return resultList
    }

    override fun getStringsFor(reference: String, origin: Origin): List<String?> =
        getValuesFor(reference, origin).map { it.toString() }.toList()

    override fun asString(): String {
        return json.value.toString()
    }


}

data class AntlrSyntaxError(val start: Point, val msg: String)

private interface StoreAntlrSyntaxErrorsForRML : AntlrJsonPathCompilerErrorListener {
    val antlrErrors: List<AntlrSyntaxError>
}

private fun capturingAntlrJsonPathCompilerErrorListener(): StoreAntlrSyntaxErrorsForRML {
    return object : AntlrJsonPathCompilerErrorListener, BaseErrorListener(), StoreAntlrSyntaxErrorsForRML {
        override fun unknownFunctionExtension(functionExtensionName: String) {
            println(
                "Unknown JSONPath function extension: \"$functionExtensionName\""
            )
        }

        override fun invalidFunctionExtensionForTestExpression(functionExtensionName: String) {
            println(
                "Invalid JSONPath function extension return type for test expression: \"$functionExtensionName\""
            )
        }

        override fun invalidFunctionExtensionForComparable(functionExtensionName: String) {
            println(
                "Invalid JSONPath function extension return type for comparable expression: \"$functionExtensionName\""
            )
        }

        override fun invalidArglistForFunctionExtension(
            functionExtensionName: String,
            functionExtensionImplementation: JsonPathFunctionExtension<*>,
            coercedArgumentTypes: List<Pair<JsonPathFilterExpressionType?, String>>
        ) {
            println(
                "Invalid arguments for function extension \"$functionExtensionName\": Expected: <${
                    functionExtensionImplementation.argumentTypes.joinToString(
                        ", "
                    )
                }>, but received <${
                    coercedArgumentTypes.map { it.first }.joinToString(", ")
                }>: <${coercedArgumentTypes.map { it.second }.joinToString(", ")}>"
            )
        }

        override fun invalidTestExpression(testContextString: String) {
            println(
                "Invalid test expression: $testContextString"
            )
        }


        override val antlrErrors = mutableListOf<AntlrSyntaxError>()

        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            println("Syntax error $line:$charPositionInLine $msg")
            antlrErrors.add(AntlrSyntaxError(start = Point(line, charPositionInLine), msg))
        }
    }
}