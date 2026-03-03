package burp.ls

import burp.model.Iteration
import burp.reporting.Origin
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.XdmItem

class XMLIteration(
    private val node: XdmItem,
    nulls: Set<Any?>,
    private val prefixMap: Map<String, String>?
) : Iteration(nulls) {
    override fun getValuesFor(reference: String?, origin: Origin): List<Any?> {
        // We need to explicitly convert the objects
        // to strings because RML has not worked out
        // "6.6.1 Automatically deriving datatypes" yet
        return getStringsFor(reference, origin)
    }

    override fun getStringsFor(reference: String?, origin: Origin): List<String> {
        val l2 = mutableListOf<String>()
        try {
            val xPath = Processor(false).newXPathCompiler()
            if (prefixMap != null) {
                for ((prefix, uri) in prefixMap) {
                    if (prefix != null && uri != null) {
                        xPath.declareNamespace(prefix, uri)
                    }
                }
            }

            val referenceCompiled = xPath.compile(reference)
            val selector = referenceCompiled.load()
            selector.contextItem = node
            val value = selector.evaluate()

            val iter = value.iterator()
            while (iter.hasNext()) {
                val item = iter.next()
                val strValue = item.stringValue
                if (strValue != null && !nulls.contains(strValue)) {
                    l2.add(strValue)
                }
            }
        } catch (e: Exception) {
            // No data, silently ignore
            e.printStackTrace()
        }
        return l2
    }

    override fun asString(): String {
        return node.toString()
    }
}
