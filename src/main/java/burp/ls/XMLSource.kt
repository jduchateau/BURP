package burp.ls

import burp.model.Iteration
import burp.util.SimpleNamespaceContext
import org.apache.commons.io.IOUtils
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathEvaluationResult.XPathResultType
import javax.xml.xpath.XPathFactory
import javax.xml.xpath.XPathNodes

internal class XMLSource : FileBasedLogicalSource() {
    @JvmField
    var prefixMap: Map<String, String>? = null

    private var iterations: MutableList<XMLIteration>? = null;

    override fun iterator(): Iterator<XMLIteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf()

                val contents = Files.readString(Paths.get(decompressedFile), encoding)

                val builderFactory = DocumentBuilderFactory.newInstance()
                if (prefixMap != null) {
                    // Required for prefix evaluation of XPath expression
                    builderFactory.isNamespaceAware = true
                }
                val builder = builderFactory.newDocumentBuilder()
                val xmlDocument = builder.parse(IOUtils.toInputStream(contents, encoding))

                val xPath = XPathFactory.newInstance().newXPath()
                if (prefixMap != null) {
                    val namespaces = SimpleNamespaceContext(prefixMap!!)
                    xPath.namespaceContext = namespaces
                }

                val nodes = xPath.compile(iterator).evaluate(xmlDocument, XPathConstants.NODESET) as NodeList

                for (i in 0..<nodes.length) {
                    val node = nodes.item(i)
                    iterations!!.add(XMLIteration(node, nulls, prefixMap))
                }
            }
            return iterations!!.iterator()
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }
}

internal class XMLIteration(
    private val node: Node?,
    nulls: MutableSet<Any?>?,
    private val prefixMap: Map<String, String>?
) : Iteration(nulls) {
    override fun getValuesFor(reference: String?): MutableList<Any?> {
        // We need to explicitly convert the objects
        // to strings because RML has not worked out
        // "6.6.1 Automatically deriving datatypes" yet
        val returnValues: MutableList<Any?> = ArrayList<Any?>()
        try {
            val xPath = XPathFactory.newInstance().newXPath()
            if (prefixMap != null) {
                val namespaces = SimpleNamespaceContext(prefixMap)
                xPath.namespaceContext = namespaces
            }

            val evaluation = xPath.compile(reference).evaluateExpression(node)

            when (evaluation.type()) {
                XPathResultType.STRING -> {
                    if (!nulls.contains(evaluation)) returnValues.add(evaluation.value())
                }

                XPathResultType.NUMBER, XPathResultType.BOOLEAN -> {
                    val evalStr = evaluation.value()
                    if (!nulls.contains(evalStr)) returnValues.add(evalStr)
                }

                XPathResultType.NODESET -> {
                    val nodes = evaluation.value() as XPathNodes
                    nodes.forEach { node ->
                        if (node.textContent != null && !nulls.contains(node.textContent))
                            returnValues.add(node.textContent)
                    }

                }

                XPathResultType.NODE -> {
                    val node = evaluation.value() as Node
                    if (node.textContent != null && !nulls.contains(node.textContent)) returnValues.add(node.textContent)
                }

                else -> throw Exception("Unsupported XPath object of type ${evaluation.type()}: ${evaluation.value()}")
            }
        } catch (e: Exception) {
            // No data, silently ignore
            e.printStackTrace()
        }
        return returnValues
    }

    override fun getStringsFor(reference: String?): MutableList<String?> = getValuesFor(reference)
        .map { it?.toString() }
        .toMutableList()
}
