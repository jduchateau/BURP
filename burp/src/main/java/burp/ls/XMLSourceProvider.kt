package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.model.Reference
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import burp.vocabularies.RML
import burp.vocabularies.Rml
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import rdfkt.JenaNamedNode
import rdfobjectloader.RDFPointer
import rdfobjectloader.StatementPart
import java.io.StringReader
import java.nio.file.Path
import javax.xml.transform.stream.StreamSource

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
open class XMLSourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: rdf.Term): Boolean {
        if (Rml.XPath == referenceFormulation.value) return true
        val jenaNode = (referenceFormulation as? JenaNamedNode)?.node
        return jenaNode?.hasProperty(RDF.type, RML.XPathReferenceFormulation) == true
    }

    override fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val (file, origin) = getFile(ls.getPropertyResourceValue(RML.source), mappingDirectory, currentWorkingDirectory)
        val source = XMLSource()
        source.file = file
        source.fileOriginStmts = origin
        source.iterator = ls.getProperty(RML.iterator).getLiteral().getString()
        source.iteratorOrigin = Origin(ls.getProperty(RML.iterator), StatementPart.Object)
        source.encoding = getEncoding(ls)
        source.compression = getCompression(ls)
        source.nulls.addAll(getNullValues(ls))
        val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)
        if (referenceFormulation.hasProperty(RDF.type, RML.XPathReferenceFormulation)) {
            source.prefixMap = getPrefixMap(ls)
        }
        return source
    }

    override fun parseStringPayload(
        payload: String,
        iterator: String?,
        referenceFormulationOrigin: Origin?
    ): List<Iteration> {
        return try {
            val xmlDocument = XMLSource.documentBuilder.build(StreamSource(StringReader(payload)))
            val xPathCompiler = XMLSource.processor.newXPathCompiler()

            requireNotNull(iterator) {
                throw BurpException(
                    RmlError(
                        "Iterator is null",
                        referenceFormulationOrigin,
                        RER.MappingError
                    )
                )
            }

            val selector = xPathCompiler.compile(iterator).load()
            selector.contextItem = xmlDocument
            val nodes = selector.evaluate()

            nodes.iterator().asSequence().map {
                XMLIteration(it, emptySet(), xPathCompiler)
            }.toList()
        } catch (e: Exception) {
            if (e is BurpException) throw e
            throw BurpException(
                RmlError(
                    "Unexpected Error while changing iterator to type XPath, iteration content $payload.",
                    referenceFormulationOrigin,
                    RER.Error,
                    e
                )
            )
        }
    }

    override fun buildReference(
        reference: String,
        referenceOrigin: RDFPointer,
        referenceFormulationOrigin: Origin?
    ): Reference {
        return XMLReference(reference, referenceOrigin)
    }
}

/**
 * Generates prefix map from rml:namespace definitions
 *
 * ls should look like:
 *
 * ls rml:referenceFormulation [
 *    rml:namespace [
 *      rml:namespacePrefix "prefix"
 *      rml:namespaceURL "url"
 *    ]
 * ]
 *
 */
fun getPrefixMap(ls: Resource): Map<String, String> {
    // Get XPathReferenceFormulation
    val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)
    // Set the map of namespaces for XPath iteration
    val properties = referenceFormulation.listProperties(RML.namespace)
    val prefixMap = mutableMapOf<String, String>()
    while (properties.hasNext()) {
        val statement = properties.next()
        val namespace = statement.resource
        val prefixValue = namespace.getProperty(RML.namespacePrefix).literal.string
        val urlValue = namespace.getProperty(RML.namespaceURL).literal.string
        prefixMap[prefixValue] = urlValue
    }
    return prefixMap
}