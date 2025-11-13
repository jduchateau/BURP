package burp.ls

import burp.model.LogicalSource
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
        val source = XMLSource()
        source.file = getFile(ls, mappingDirectory, currentWorkingDirectory)
        source.iterator = ls.getProperty(RML.iterator).getLiteral().getString()
        source.encoding = getEncoding(ls)
        source.compression = getCompression(ls)
        source.nulls.addAll(getNullValues(ls))
        val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)
        if (referenceFormulation.hasProperty(RDF.type, RML.XPathReferenceFormulation)) {
            source.prefixMap = getPrefixMap(ls)
        }
        return source
    }

    companion object {
        // Generates prefix map from rml:namespace definitions
        @JvmStatic
        protected fun getPrefixMap(ls: Resource): HashMap<String?, String?> {
            // Get XPathRerenceFormulation
            val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)
            // Set map of namespaces for XPath iteration
            val properties = referenceFormulation.listProperties(RML.namespace)
            val prefixMap = HashMap<String?, String?>()
            while (properties.hasNext()) {
                val statement = properties.next()
                val namespace = statement.getResource()
                prefixMap.put(
                    namespace.getProperty(RML.namespacePrefix).getLiteral().getString(),
                    namespace.getProperty(RML.namespaceURL).getLiteral().getString()
                )
            }
            return prefixMap
        }
    }
}
