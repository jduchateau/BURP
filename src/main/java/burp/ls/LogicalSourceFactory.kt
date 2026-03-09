package burp.ls

import at.asitplus.jsonpath.JsonPath
import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.*
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.opencsv.CSVReader
import kotlinx.serialization.json.Json
import org.apache.jena.rdf.model.Resource
import java.io.StringReader
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import javax.xml.transform.stream.StreamSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object LogicalSourceFactory {

    private val LOADER: ServiceLoader<LogicalSourceProvider> = ServiceLoader.load(LogicalSourceProvider::class.java)

    fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val stmt = ls.getProperty(RML.referenceFormulation)
        val referenceFormulation = stmt.getObject().asResource()
        for (provider in LOADER) {
            if (provider.supports(referenceFormulation)) {
                return provider.create(ls, mappingDirectory, currentWorkingDirectory)
            }
        }

        val supported = LOADER.stream()
            .map { p: ServiceLoader.Provider<LogicalSourceProvider?>? -> p!!.type().getName() }
            .collect(Collectors.joining(", "))
        val supportedMessage =
            if (supported.isNotEmpty()) "Are supported: $supported."
            else "None are supported, provide a `burp.ls.LogicalSourceProvider` in class path."

        throw BurpException(
            UnsupportedMapping(
                "Reference formulation not supported: $referenceFormulation. $supportedMessage",
                Origin(stmt, StatementPart.Object)
            )
        )
    }


    @OptIn(ExperimentalContracts::class)
    private fun requireNonNullIterator(iterator: String?): String {
        contract { returns() implies (iterator != null) }

        return requireNotNull(iterator) {
            throw BurpException(
                RmlError(
                    "Iterator is null",
                    null,//TODO track origin of IterableField
                    RER.MappingError
                )
            )
        }
    }

    fun changeIterator(iterationAsString: String, referenceFormulation: Resource, iterator: String?): List<Iteration> {


        try {
            if (RML.JSONPath.equals(referenceFormulation)) {
                // Create JSON iterations
                val jsonContent = Json.parseToJsonElement(iterationAsString)
                requireNonNullIterator(iterator)
                val results = JsonPath(iterator).query(jsonContent)
                // TODO: How do we provide null values?
                return results.map { JSONIterationRFC(it, emptySet()) }.toList()
            } else if (RML.CSV.equals(referenceFormulation)) {
                // Create CSV iterations
                val reader = CSVReader(StringReader(iterationAsString))
                val all = reader.readAll()
                reader.close()
                val header = all.removeAt(0)
                return all.map { CSVIteration(header, it, emptySet<Any>()) }.toList()

            } else if (RML.XPath.equals(referenceFormulation)) {
                // Create XPATH iterations
                val xmlDocument = XMLSource.documentBuilder.build(StreamSource(StringReader(iterationAsString)))
                val xPathCompiler = XMLSource.processor.newXPathCompiler()
                requireNonNullIterator(iterator)
                val selector = xPathCompiler.compile(iterator).load()
                selector.contextItem = xmlDocument
                val nodes = selector.evaluate()

                return nodes.iterator().asSequence().map {
                    // TODO: How do we provide null values?
                    // TODO: How do we provide the prefix mappings?
                    XMLIteration(it, emptySet(), xPathCompiler)
                }.toList()
            }
        } catch (e: Exception) {
            throw BurpException(
                RmlError(
                    "Unexpected Error while changing iterator to $iterator type $referenceFormulation, iteration content $iterationAsString.",
                    null,
                    RER.Error,
                    e
                )
            )
        }

        throw BurpException(
            RmlError(
                "Other reference formulations for iterable fields are not yet supported: $referenceFormulation",
                null,
                RER.UnsupportedMapping
            )
        )
    }
}