package burp.ls

import burp.model.LogicalSource
import burp.vocabularies.RML
import burp.vocabularies.UCOCore
import burp.vocabularies.UCOObservable
import burp.vocabularies.YS
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import java.nio.file.Path

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
class NetconfQuerySourceProvider : XMLSourceProvider() {
    override fun supports(referenceFormulation: Resource): Boolean {
        return referenceFormulation.hasProperty(RDF.type, YS.NetconfQuerySource)
    }

    override fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val s = ls.getPropertyResourceValue(RML.source)
        // Instantiate Netconf source based on Netconf operation
        val source = NetconfQuerySource()
        val datastore = s.getPropertyResourceValue(YS.sourceDatastore)
        source.datastoreType = datastore.getPropertyResourceValue(RDF.type)
        // Get YANG Server address connection details
        val server = datastore.getPropertyResourceValue(YS.server)
        val socketAddress = server.getPropertyResourceValue(YS.socketAddress)
        source.endpoint = socketAddress.getProperty(UCOObservable.addressValue).getLiteral().getString()
        // Get YANG Server authentication details
        val serverAccount = server.getPropertyResourceValue(YS.serverAccount)
        source.username = serverAccount.getProperty(YS.username).getLiteral().getString()
        val accountAuthentication = serverAccount.getPropertyResourceValue(UCOCore.hasFacet)
        source.password = accountAuthentication.getProperty(UCOObservable.password).getLiteral().getString()
        // Get operation filter
        source.filter = s.getPropertyResourceValue(YS.filter)
        // Set XPath for RML iterator
        source.iterator = ls.getProperty(RML.iterator).getLiteral().getString()
        // Set map of prefixes for RML iterations
        source.rmlPrefixMap = getPrefixMap(ls)
        return source
    }
}
