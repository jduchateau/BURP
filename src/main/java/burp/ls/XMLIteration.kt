package burp.ls

import burp.model.Iteration
import burp.reporting.Origin
import net.sf.saxon.s9api.XPathCompiler
import net.sf.saxon.s9api.XdmItem

class XMLIteration(
    private val node: XdmItem,
    nulls: Set<Any?>,
    private val xPathCompiler: XPathCompiler
) : Iteration(nulls) {
    override fun getValuesFor(reference: String?, origin: Origin): List<Any?> {
        // We need to explicitly convert the objects
        // to strings because RML has not worked out
        // "6.6.1 Automatically deriving datatypes" yet
        return getStringsFor(reference, origin)
    }

    override fun getStringsFor(reference: String?, origin: Origin): List<String> {
//        try {
        val referenceCompiled = xPathCompiler.compile(reference)
        val selector = referenceCompiled.load()
        selector.contextItem = node
        val value = selector.evaluate()

        val results = value.iterator().asSequence().mapNotNull {
            val strValue = it.stringValue
            if (strValue != null && !nulls.contains(strValue)) strValue else null
        }.toList()
//        } catch (e: Exception) {
//            // No data, silently ignore
//            e.printStackTrace()
//        }
        return results
    }

    override fun asString(): String {
        return node.toString()
    }
}
