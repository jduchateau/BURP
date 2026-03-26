package burp.ls

import burp.model.Iteration
import net.sf.saxon.s9api.XPathCompiler
import net.sf.saxon.s9api.XdmItem

class XMLIteration(
    val node: XdmItem,
    nulls: Set<Any?>,
    val xPathCompiler: XPathCompiler
) : Iteration(nulls) {


    override fun asString(): String {
        return node.toString()
    }
}
