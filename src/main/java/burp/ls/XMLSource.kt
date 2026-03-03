package burp.ls

import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.RmlError
import burp.vocabularies.RER
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.SaxonApiException
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.transform.stream.StreamSource

class XMLSource : FileBasedLogicalSource() {
    var prefixMap: Map<String, String>? = null

    @Throws(BurpException::class)
    override fun iterator(): Iterator<Iteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf()

                val processor = Processor(false)
                val documentBuilder = processor.newDocumentBuilder()

                val xmlDocument = Files.newBufferedReader(Paths.get(getDecompressedFile()), encoding).use { reader ->
                    documentBuilder.build(StreamSource(reader))
                }

                val xPathCompiler = processor.newXPathCompiler()
                if (prefixMap != null) {
                    for ((prefix, uri) in prefixMap!!) {
                        xPathCompiler.declareNamespace(prefix, uri)
                    }
                }

                val selector = xPathCompiler.compile(iterator).load()
                selector.contextItem = xmlDocument
                val nodes = selector.evaluate()

                nodes.iterator().forEach { iterations!!.add(XMLIteration(it, nulls, prefixMap)) }
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
}

