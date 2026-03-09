package burp.ls

import burp.model.LogicalSource
import burp.reporting.Origin
import burp.reporting.StatementPart
import burp.vocabularies.RML
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import java.nio.file.Path

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
open class XMLSourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: Resource): Boolean {
        return RML.XPath == referenceFormulation
                || referenceFormulation.hasProperty(RDF.type, RML.XPathReferenceFormulation)
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