package burp.ls

import at.asitplus.jsonpath.JsonPath
import at.asitplus.jsonpath.core.*
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompiler
import at.asitplus.jsonpath.implementation.AntlrJsonPathCompilerErrorListener
import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.*
import burp.vocabularies.RER
import burp.vocabularies.RML
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
        }
    }
}

class JSONSourceRFC : FileBasedLogicalSource() {
    lateinit var iterator: String
    var iteratorOrigin: Origin? = null

    override fun iterator(): Iterator<JSONIteration> {
        val contents = Files.readString(Paths.get(getDecompressedFile()), encoding)
        val jsonContent = Json.parseToJsonElement(contents)
        val results = JsonPath(
            iterator, AntlrJsonPathCompiler(errorListener = capturingAntlrJsonPathCompilerErrorListener())
        ).query(jsonContent)
        return results.map { JSONIteration(it, nulls) }.iterator()
    }

    override var referenceFormulation: Resource
        get() = RML.JSONPath
        set(value) {}

    override fun buildReference(reference: String, origin: Origin): burp.model.Reference {
        return JSONPathReference(reference, origin)
    }
}

class JSONPathReference(reference: String?, origin: Origin) : burp.model.Reference(reference, origin) {
    private val antlrErrorListener = capturingAntlrJsonPathCompilerErrorListener()
    private val compiledPath: JsonPath? = try {
        if (reference != null) JsonPath(reference, AntlrJsonPathCompiler(errorListener = antlrErrorListener)) else null
    } catch (ex: Exception) {
        when (ex) {
            is JsonPathCompilerException -> {
                if (antlrErrorListener.antlrErrors.isEmpty()) {
                    throw BurpException(
                        RmlError(
                            "Syntax error in JSONPath `$reference`", origin, RER.ReferenceFormulationSyntaxError, ex
                        )
                    )
                } else {
                    val antlrError = antlrErrorListener.antlrErrors.first()
                    val literalPart = (origin.sourceStatements?.firstOrNull()) as? LiteralPart
                    val error = RmlError(
                        "Syntax error in JSONPath `$reference` at ${antlrError.start.displayLine}:${antlrError.start.column}: ${antlrError.msg}",
                        origin.copy(
                            sourceStatements = buildList {
                                if (literalPart != null)
                                    add(
                                        LiteralPart(
                                            literalPart.stmt,
                                            literalPart.objectRange + PointRange(antlrError.start)
                                        )
                                    )
                            }
                        ),
                        RER.ReferenceFormulationSyntaxError
                    )
                    throw BurpException(error)
                }
            }
            is BurpException -> throw ex
            else -> throw BurpException(UnexpectedError(ex, origin))
        }
    }

    override fun getValues(i: Iteration): List<Any?> {
        require(i is JSONIteration)
        if (compiledPath == null) return emptyList()

        val resultList: MutableList<Any?> = mutableListOf()
        try {
            val entries = compiledPath.query(i.json.value)
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
                        if (i.nulls.contains(content) != true) resultList.add(content)
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is JsonPathQueryException) {
                throw BurpException(
                    RmlError(
                        "Execution error in JSONPath `$reference`", origin, RER.ReferenceFormulationExecutionError, ex
                    )
                )
            } else if (ex is BurpException) {
                throw ex
            } else {
                throw BurpException(UnexpectedError(ex, origin))
            }
        }
        return resultList
    }
}

class JSONIteration(val json: NodeListEntry, nulls: Set<Any?>) : Iteration(nulls) {
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
            functionExtensionImplementation: JsonPathFunctionExtension,
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
            antlrErrors.add(AntlrSyntaxError(start = Point(line - 1, charPositionInLine), msg))
        }
    }
}