package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError.Companion.SourceAccessError
import burp.reporting.StatementParts
import burp.util.getDecompressedFile
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal abstract class FileBasedLogicalSource : LogicalSource() {
    @JvmField
    protected var iterations: MutableList<Iteration?>? = null

    @JvmField
    var file: SourceFile? = null

    @JvmField
    var fileOriginStmts: List<StatementParts> = emptyList()

    @JvmField
    var encoding: Charset = StandardCharsets.UTF_8
    var compression: Resource = RML.none

    fun getDecompressedFile(): String {
        val origin = Origin(this, fileOriginStmts)
            val fileFile =  file!!.getFile(fileOriginStmts)
            if (fileFile == null || !fileFile.exists()) {
                throw BurpException(SourceAccessError("Cannot obtain file $file", origin, null))
            }
            return getDecompressedFile(fileFile.absolutePath, compression, origin)


    }
}
