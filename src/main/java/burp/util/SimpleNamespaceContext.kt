package burp.util

import javax.xml.namespace.NamespaceContext

// Kudos to https://stackoverflow.com/a/6392700
class SimpleNamespaceContext(val prefMap: Map<String, String>) : NamespaceContext {

    override fun getNamespaceURI(prefix: String?): String? = prefMap[prefix]

    override fun getPrefix(uri: String?): String = throw UnsupportedOperationException()

    override fun getPrefixes(uri: String?): Iterator<String> = throw UnsupportedOperationException()
}
