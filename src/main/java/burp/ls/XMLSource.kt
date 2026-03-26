package burp.ls

import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import burp.vocabularies.RML
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.XPathCompiler
import org.apache.jena.rdf.model.Resource
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.transform.stream.StreamSource

class XMLSource : FileBasedLogicalSource() {
    var iterator: String? = null
    var iteratorOrigin: Origin? = null

    var prefixMap: Map<String, String>? = null

    private var xPathCompiler: XPathCompiler? = null

    @Throws(BurpException::class)
    override fun iterator(): Iterator<Iteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf()

                val xmlDocument = Files.newBufferedReader(Paths.get(getDecompressedFile()), encoding).use { reader ->
                    documentBuilder.build(StreamSource(reader))
                }

                xPathCompiler = processor.newXPathCompiler()
                if (prefixMap != null) {
                    for ((prefix, uri) in prefixMap!!) {
                        xPathCompiler!!.declareNamespace(prefix, uri)
                    }
                }

                val selector = xPathCompiler!!.compile(iterator).load()
                selector.contextItem = xmlDocument
                val nodes = selector.iterator()

                nodes.forEach { iterations!!.add(XMLIteration(it, nulls, xPathCompiler!!)) }
            }
            return iterations!!.iterator()
        } catch (e: SaxonApiException) {
            throw BurpException(
                RmlError(
                    e.message!!,
                    iteratorOrigin,
                    RER.ReferenceFormulationSyntaxError,
                    e,
                    mutableMapOf()
                )
            )
        } catch (e: Exception) {
            throw BurpException(
                RmlError(
                    e.message!!,
                    iteratorOrigin,
                    RER.ReferenceFormulationExecutionError,
                    e,
                    mutableMapOf()
                )
            )
        }
    }

    override var referenceFormulation: Resource
        get() = RML.XPath
        set(value) {}

    override fun buildReference(reference: String, origin: Origin): burp.model.Reference {
        return XMLReference(reference, origin)
    }

    companion object {
        val processor = Processor(false)
        val documentBuilder = processor.newDocumentBuilder()
    }
}

class XMLReference(reference: String?, origin: Origin) : burp.model.Reference(reference, origin) {
    override fun getValues(i: Iteration): List<Any?> {
        require(i is XMLIteration)
        return try {
            val selector = i.xPathCompiler.compile(reference).load()
            selector.contextItem = i.node
            val nodes = selector.iterator()
            val l = mutableListOf<String>()
            nodes.forEach { l.add(it.stringValue) }
            l
        } catch (e: Exception) {
            throw BurpException(
                RmlError(
                    "Error executing XPath: $reference on node $i.",
                    origin,
                    RER.ReferenceFormulationExecutionError,
                    e
                )
            )
        }
    }
}

