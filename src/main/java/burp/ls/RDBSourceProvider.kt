package burp.ls

import burp.vocabularies.D2RQ
import burp.vocabularies.RML
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import java.nio.file.Path

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
open class RDBQuerySourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: Resource): Boolean {
        return RML.SQL2008Query == referenceFormulation
    }

    override fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): RDBSource {
        val source = RDBSource()

        source.referenceFormulation = RML.SQL2008Query

        val sourceNode = ls.getPropertyResourceValue(RML.source)
        val jdbcDSNStmt =  requireNotNull(sourceNode.getProperty(D2RQ.jdbcDSN)) { "RDB source must have a d2rq:jdbcDSN property." }
        val jdbcDSNLiteral = jdbcDSNStmt.literal.string
        source.jdbcDSN = jdbcDSNLiteral

        val jdbcDriverStmt = sourceNode.getProperty(D2RQ.jdbcDriver)
        source.jdbcDriver = jdbcDriverStmt?.literal?.string

        val usernameStmt = sourceNode.getProperty(D2RQ.username)
        source.username = usernameStmt?.literal?.string

        val passwordStmt = sourceNode.getProperty(D2RQ.password)
        source.password = passwordStmt?.literal?.string

        val query = ls.getProperty(RML.iterator).literal.string

        // Apache Jena "escapes" double quotes, so "Name" becomes \"Name\" which is internally stored as \\"Name\\".
        // We thus need to remove occurrences of \\
        source.query = query.replace("\\", "")

        source.nulls.addAll(getNullValues(ls))

        return source
    }
}

@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
class RDBTableSourceProvider : RDBQuerySourceProvider() {
    override fun supports(referenceFormulation: Resource): Boolean {
        return RML.SQL2008Table == referenceFormulation
    }

    override fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): RDBSource {
        val source = super.create(ls, mappingDirectory, currentWorkingDirectory)
        source.referenceFormulation = RML.SQL2008Table
        source.query = "(SELECT * FROM " + source.query + ")"
        return source
    }
}