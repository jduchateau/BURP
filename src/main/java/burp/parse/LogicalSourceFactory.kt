package burp.parse

import burp.ls.*
import burp.model.LogicalSource
import burp.util.Util
import burp.vocabularies.*
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VOID
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object LogicalSourceFactory {
    fun createCSVSource(ls: Resource, mpath: String?): LogicalSource {
        val source = CSVSource()

        if (ls.getPropertyResourceValue(RML.source).hasProperty(RDF.type, CSVW.Table)) {
            val sourceNode = ls.getPropertyResourceValue(RML.source)

            val file = getFile(ls)
            source.file = getAbsoluteOrRelative(file, mpath)

            // IF IT IS A CSVW TABLE, THEN LOOK FOR THE ENCODING IN THE DIALECT
            if (sourceNode.hasProperty(CSVW.dialect)) {
                val dialectNode = sourceNode.getPropertyResourceValue(CSVW.dialect)
                if (dialectNode.hasProperty(CSVW.encoding) && !ls.hasProperty(RML.encoding)) {
                    val e = dialectNode.getProperty(CSVW.encoding).string
                    when (e) {
                        "UTF-8" -> source.encoding = StandardCharsets.UTF_8
                        "UTF-16" -> source.encoding = StandardCharsets.UTF_16
                        else -> throw RuntimeException("Provided Character Set $dialectNode not supported.")
                    }
                }

                if (dialectNode.hasProperty(CSVW.delimiter)) {
                    // TODO: According to CSVW, the delimiter is a string. But all examples are chars.
                    source.delimiter = dialectNode.getProperty(CSVW.delimiter).char
                }

                if (dialectNode.hasProperty(CSVW.header)) {
                    source.firstLineIsHeader = dialectNode.getProperty(CSVW.header).boolean
                }
            } else {
                source.encoding = StandardCharsets.UTF_8
            }

            // csv:null can be defined on csvw:Table/Column etc but not in csvw:Dialect according to the csvw spec
            // rml:null take precedence over csvw:null
            if (sourceNode.hasProperty(CSVW.NULL))
                source.nulls = sourceNode.listProperties(CSVW.NULL).mapWith { it.literal.value }.toSet()
            if (sourceNode.hasProperty(RML.NULL))
                source.nulls = getNullValuesSource(sourceNode).toSet()

            source.compression = getCompression(ls)
        } else {
            // WE HAVE A SIMPLE CSV FILE
            val file = getFile(ls)
            source.file = getAbsoluteOrRelative(file, mpath)
            source.compression = getCompression(ls)
        }

        source.encoding = getEncoding(ls)
        source.nulls.addAll(getNullValues(ls))

        return source
    }

    fun createJSONSource(ls: Resource, mpath: String?): LogicalSource {
        val file = getFile(ls)
        val iterator = ls.getProperty(RML.iterator).literal.string
        val source = JSONSource()
        source.file = getAbsoluteOrRelative(file, mpath)
        source.iterator = iterator
        source.encoding = getEncoding(ls)
        source.compression = getCompression(ls)
        source.nulls.addAll(getNullValues(ls))
        return source
    }

    fun createXMLSource(ls: Resource, mpath: String?): LogicalSource {
        val file = getFile(ls)
        val iterator = ls.getProperty(RML.iterator).literal.string
        val source = XMLSource()
        source.file = getAbsoluteOrRelative(file, mpath)
        source.iterator = iterator
        source.encoding = getEncoding(ls)
        source.compression = getCompression(ls)
        source.nulls.addAll(getNullValues(ls))
        val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)
        if (referenceFormulation.hasProperty(RDF.type, RML.XPathReferenceFormulation)) {
            source.prefixMap = getPrefixMap(ls)
        }
        return source
    }

    private fun createRDBSource(sourceNode: Resource): RDBSource {
        val jdbcDSN = sourceNode.getProperty(D2RQ.jdbcDSN)?.literal?.string
            ?: throw RuntimeException("RDB source must have a d2rq:jdbcDSN property")
        val jdbcDriver = sourceNode.getProperty(D2RQ.jdbcDriver)?.literal?.string
        val username = sourceNode.getProperty(D2RQ.username)?.literal?.string
        val password = sourceNode.getProperty(D2RQ.password)?.literal?.string

        val source = RDBSource()
        source.jdbcDSN = jdbcDSN
        source.jdbcDriver = jdbcDriver
        source.username = username
        source.password = password
        source.nulls.addAll(getNullValuesSource(sourceNode))
        return source
    }

    fun createSQL2008TableSource(ls: Resource, mpath: String?): LogicalSource {
        val s = ls.getPropertyResourceValue(RML.source)
        val source = createRDBSource(s)

        val t = ls.getProperty(RML.iterator)
        val query = "(SELECT * FROM " + t.literal + ")"

        // Apache jena "escapes" double quotes, so "Name" becomes \"Name\"
        // which is internally stored as \\"Name\\". We thus need to remove
        // occurrences of \\
        source.query = query.replace("\\", "")

        source.nulls.addAll(getNullValues(ls))

        return source
    }

    fun createSQL2008QuerySource(ls: Resource, mpath: String?): LogicalSource {
        val s = ls.getPropertyResourceValue(RML.source)
        val source = createRDBSource(s)

        val t = ls.getProperty(RML.iterator)
        val query = t.literal.string

        // Apache jena "escapes" double quotes, so "Name" becomes \"Name\"
        // which is internally stored as \\"Name\\". We thus need to remove
        // occurrences of \\
        source.query = query.replace("\\", "")

        source.nulls.addAll(getNullValues(ls))

        return source
    }

    fun createSPARQLSource(ls: Resource, mpath: String?, isTSV: Boolean): LogicalSource {
        val iterator = ls.getProperty(RML.iterator).literal.string

        val s = ls.getPropertyResourceValue(RML.source)

        if (s.hasProperty(RDF.type, VOID.Dataset)) {
            val source = SPARQLFileSource(isTSV)
            val file = s.getPropertyResourceValue(VOID.dataDump).uri
            source.file = getAbsoluteOrRelativeFromFileProtocol(file, mpath)
            source.compression = getCompression(ls)
            source.encoding = getEncoding(ls)
            source.iterator = iterator
            source.nulls.addAll(getNullValues(ls))
            return source
        } else if (s.hasProperty(RDF.type, SD.Service)) {
            val source = SPARQLServiceSource(isTSV)
            source.endpoint = s.getPropertyResourceValue(SD.endpoint).uri
            source.iterator = iterator
            source.nulls.addAll(getNullValues(ls))
            return source
        } else {
            // WE HAVE A SIMPLE SPARQL SOURCE
            val source = SPARQLFileSource(isTSV)
            val file = getFile(ls)
            source.file = getAbsoluteOrRelative(file, mpath)
            source.compression = getCompression(ls)
            source.encoding = getEncoding(ls)
            source.iterator = iterator
            source.nulls.addAll(getNullValues(ls))
            return source
        }
    }

    fun createNetconfQuerySource(ls: Resource): LogicalSource {
        val s = ls.getPropertyResourceValue(RML.source)
        // Instantiate Netconf source based on Netconf operation
        val source = NetconfQuerySource()
        val datastore = s.getPropertyResourceValue(YS.sourceDatastore)
        source.datastoreType = datastore.getPropertyResourceValue(RDF.type)
        // Get YANG Server address connection details
        val server = datastore.getPropertyResourceValue(YS.server)
        val socketAddress = server.getPropertyResourceValue(YS.socketAddress)
        source.endpoint = socketAddress.getProperty(UCOObservable.addressValue).literal.string
        // Get YANG Server authentication details
        val serverAccount = server.getPropertyResourceValue(YS.serverAccount)
        source.username = serverAccount.getProperty(YS.username).literal.string
        val accountAuthentication = serverAccount.getPropertyResourceValue(UCOCore.hasFacet)
        source.password = accountAuthentication.getProperty(UCOObservable.password).literal.string
        // Get operation filter
        source.filter = s.getPropertyResourceValue(YS.filter)
        // Set XPath for RML iterator
        source.rmlIterator = ls.getProperty(RML.iterator).literal.string
        // Set map of prefixes for RML iterations
        source.rmlPrefixMap = getPrefixMap(ls)
        return source
    }

    // *************************************************************************
    // *
    // * UTILITY FUNCTIONS FOR LOGICAL SOURCES
    // *
    // *************************************************************************
    private fun getFile(ls: Resource): String {
        val source = ls.getPropertyResourceValue(RML.source)

        if (source.hasProperty(RDF.type, RML.RelativePathSource)) {
            val file = source.getProperty(RML.path).literal.string

            val root = source.getPropertyResourceValue(RML.root)
            if (root != null) {
                if (RML.MappingDirectory == root) return file
                if (RML.CurrentWorkingDirectory == root) return File(
                    File(System.getProperty("user.dir")),
                    file
                ).path
                if (root.isLiteral) return File(root.asLiteral().string, file).path
                throw RuntimeException("RelativePathSource specified root value is not supported. $root")
            }

            // By default BURP treats it relative to mapping.
            return file
        }

        if (source.hasProperty(RDF.type, DCAT.Distribution)) {
            val url = source.getPropertyResourceValue(DCAT.downloadURL).uri
            return Util.downloadFile(url)
        }

        if (source.hasProperty(RDF.type, CSVW.Table)) {
            val url = source.getProperty(CSVW.url).literal.string
            return Util.downloadFile(url)
        }

        throw RuntimeException("Source from this logical source type not yet implemented")
    }

    private fun getCompression(ls: Resource): Resource? {
        var r = ls.getPropertyResourceValue(RML.source)

        r = r!!.getPropertyResourceValue(RML.compression)
        if (r == null || RML.none == r) return RML.none
        if (RML.zip == r) return RML.zip
        if (RML.gzip == r) return RML.gzip
        if (RML.targz == r) return RML.targz
        if (RML.tarxz == r) return RML.tarxz

        throw RuntimeException("Provided compression $r not supported.")
    }

    private fun getEncoding(ls: Resource): Charset {
        var r = ls.getPropertyResourceValue(RML.source)

        r = r!!.getPropertyResourceValue(RML.encoding)
        if (r == null || RML.UTF8 == r) return StandardCharsets.UTF_8
        if (RML.UTF16 == r) return StandardCharsets.UTF_16

        throw RuntimeException("Provided Character Set $r not supported.")
    }

    private fun getAbsoluteOrRelative(file: String, mpath: String?): String {
        if (File(file).isAbsolute) return file
        return File(mpath, file).absolutePath
    }

    private fun getAbsoluteOrRelativeFromFileProtocol(file: String, mpath: String?): String {
        try {
            val url = URI(file).toURL()
            if (Util.isAbsoluteAndValidIRI(file)) return file
            val abs = File(mpath, url.path).toURI().toURL().toString()
            return abs.replaceFirst("file:/".toRegex(), "")
        } catch (e: MalformedURLException) {
            throw RuntimeException("$file is not a file URL.")
        }
    }

    private fun getNullValuesSource(sourceNode: Resource): List<Any?> {
        return sourceNode.listProperties(RML.NULL).mapWith { stmt ->
            if (stmt.getObject().isResource)
            // WE ASSUME WE CAN HAVE RESOURCES AS NULL FOR SPARQL SOURCES
                stmt.getObject().asResource()
            else
                stmt.getObject().asLiteral().value

        }.toList()
    }

    private fun getNullValues(ls: Resource): List<Any?> {
        return getNullValuesSource(ls.getPropertyResourceValue(RML.source))
    }

    // Generates prefix map from rml:namespace definitions
    private fun getPrefixMap(ls: Resource): Map<String, String> {
        // Get XPathRerenceFormulation
        val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)
        // Set map of namespaces for XPath iteration
        return referenceFormulation.listProperties(RML.namespace).mapWith { stmt ->
            val namespace = stmt.resource
            val prefix = namespace.getProperty(RML.namespacePrefix).literal.string
            val ns = namespace.getProperty(RML.namespaceURL).literal.string
            prefix to ns
        }.toList().toMap()
    }
}
