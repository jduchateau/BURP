package burp.errors

import burp.model.IPlanNode
import org.apache.jena.rdf.model.Resource

class BurpException(type: Resource, message: String, node: IPlanNode?) : Exception(message)