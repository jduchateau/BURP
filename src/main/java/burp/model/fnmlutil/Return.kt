package burp.model.fnmlutil

import burp.errors.BurpException
import burp.model.IPlanNode
import burp.model.RmlOrigin
import burp.vocabularies.RMLError
import org.apache.jena.rdf.model.RDFNode

class Return(defaultValue: Any?) : IPlanNode {
    private val returns = mutableMapOf<String, Any?>()

    @JvmField
    var defaultValue: Any? = null

    init {
        this.defaultValue = defaultValue
    }

    fun get(key: String) = returns.getOrElse(key.toString()) {
        throw BurpException(RMLError.ExecutionError, "Unknown return value $key.", this)
    }


    fun put(key: String, value: Any?) = returns.put(key.toString(), value)

    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null
}
