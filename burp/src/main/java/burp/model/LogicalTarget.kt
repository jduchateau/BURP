package burp.model

import org.apache.jena.rdf.model.Resource

class LogicalTarget(
    val target: RMLTarget,
    val serialization: Resource? = null,
    val compression: Resource? = null,
    val encoding: Resource? = null
)

sealed class RMLTarget

class FilePathTarget(val path: String, val root: Resource) : RMLTarget()
