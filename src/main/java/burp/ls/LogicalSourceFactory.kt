package burp.ls

import at.asitplus.jsonpath.JsonPath
import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.reporting.StatementPart
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.opencsv.CSVReader
import kotlinx.serialization.json.Json
import org.apache.jena.rdf.model.Resource
import org.w3c.dom.NodeList
import java.io.StringReader
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.collections.emptyMap

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
        throw BurpException(
            RmlError.UnsupportedMapping(
                "Reference formulation not supported: " + referenceFormulation + ". " +
                        "Are supported: " + supported,
                Origin(stmt, StatementPart.Object)
            )
        )
    }


    fun changeIterator(iterationAsString: String, rf: Resource, iterator: String): List<Iteration> {
        try {
            if (RML.JSONPath.equals(rf)) {
                // Create JSON iterations
                val jsonContent = Json.parseToJsonElement(iterationAsString);
                val results = JsonPath(iterator).query(jsonContent);
                // TODO: How do we provide null values?
                return results.map { JSONIterationRFC(it, emptySet()) }.toList();
            } else if (RML.CSV.equals(rf)) {
                // Create CSV iterations
                val reader = CSVReader(StringReader(iterationAsString))
                val all = reader.readAll()
                reader.close()
                val header = all.removeAt(0)
                return all.map { CSVIteration(header, it, emptySet<Any>()) }.toList()

            } else if (RML.XPath.equals(rf)) {
                // Create XPATH iterations
                val builderFactory = DocumentBuilderFactory.newInstance()
                val builder = builderFactory.newDocumentBuilder()
                val xmlDocument = builder.parse((iterationAsString))
                val xPath = XPathFactory.newInstance().newXPath()
                val nodes = xPath.compile(iterator).evaluate(xmlDocument, XPathConstants.NODESET) as NodeList

                val iterations = mutableListOf<Iteration>()
                for (index in 0 until nodes.length) {
                    val node = nodes.item(index)
                    // TODO: How do we provide null values?
                    // TODO: How do we provide the prefix mappings?
                    iterations.add(XMLIteration(node, emptySet<Any>(), emptyMap<String, String>()))
                }
                return iterations
            }
        } catch (e: Exception) {
            throw BurpException(
                RmlError(
                    "Unexpected Error while changing iterator to $iterator type $rf, iteration content $iterationAsString.",
                    null,
                    RER.Error,
                    e
                )
            )
        }

        throw BurpException(
            RmlError(
                "Other reference formulations for iterable fields are not yet supported: $rf",
                null,
                RER.UnsupportedMapping
            )
        )
    }
}