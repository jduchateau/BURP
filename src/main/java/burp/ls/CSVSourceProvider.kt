package burp.ls

import burp.model.LogicalSource
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.StatementPart
import burp.reporting.UnsupportedMapping
import burp.vocabularies.CSVW
import burp.vocabularies.RML
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.RDF
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.function.Consumer

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
class CSVSourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: Resource): Boolean {
        return referenceFormulation == RML.CSV
    }

    override fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val s = ls.getPropertyResourceValue(RML.source)
        val source = CSVSource()
        val (file, origin) = getFile(s, mappingDirectory, currentWorkingDirectory)
        source.file = file
        source.fileOriginStmts = origin
        source.compression = getCompression(s)
        if (s.hasProperty(RDF.type, CSVW.Table)) {
            // IF IT IS A CSVW TABLE, THEN LOOK FOR THE ENCODING IN THE DIALECT

            if (s.hasProperty(CSVW.dialect)) {
                val r = s.getPropertyResourceValue(CSVW.dialect)
                if (r.hasProperty(CSVW.encoding) && !ls.hasProperty(RML.encoding)) {
                    val encodingStmt = r.getProperty(CSVW.encoding)
                    val encoding = encodingStmt.string
                    source.encoding = when (encoding) {
                        "UTF-8" -> StandardCharsets.UTF_8
                        "UTF-16" -> StandardCharsets.UTF_16
                        else -> throw BurpException(
                            UnsupportedMapping(
                                "Provided Character Set $r not supported.",
                                Origin(encodingStmt, StatementPart.Predicate, StatementPart.Object)
                            )
                        )
                    }
                }

                if (r.hasProperty(CSVW.delimiter)) {
                    // TODO: According to CSVW, the delimiter is a string. But all examples are chars.
                    source.delimiter = r.getProperty(CSVW.delimiter).getChar()
                }

                if (r.hasProperty(CSVW.header)) {
                    source.firstLineIsHeader = r.getProperty(CSVW.header).getBoolean()
                }

                if (r.hasProperty(CSVW.NULL) && !ls.hasProperty(RML.NULL)) {
                    r.listProperties(RML.NULL).forEach(Consumer { t: Statement? ->
                        if (t!!.getObject().isResource()) {
                            // WE ASSUME WE CAN HAVE RESOURCES AS NULL FOR
                            // SPARQL SOURCES
                            source.nulls.add(t.getObject().asResource())
                        } else {
                            source.nulls.add(t.getObject().asLiteral().getValue())
                        }
                    })
                }

                // TODO: Natural RDF mapping of CSV values
            } else {
                source.encoding = StandardCharsets.UTF_8
            }
        }

        source.encoding = getEncoding(ls)
        source.nulls.addAll(getNullValues(ls))

        return source
    }
}
