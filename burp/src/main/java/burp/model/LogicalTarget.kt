package burp.model

import rdf.Term

class LogicalTarget(
    val target: RMLTarget,
    val serialization: Term? = null,
    val compression: Term? = null,
    val encoding: Term? = null
)

sealed class RMLTarget

class FilePathTarget(val path: String, val root: Term) : RMLTarget()
