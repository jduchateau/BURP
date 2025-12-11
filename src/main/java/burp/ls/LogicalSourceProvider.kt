package burp.ls

import burp.model.LogicalSource
import org.apache.jena.rdf.model.Resource
import java.nio.file.Path

interface LogicalSourceProvider {
    fun supports(referenceFormulation: Resource): Boolean
    fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource
}
