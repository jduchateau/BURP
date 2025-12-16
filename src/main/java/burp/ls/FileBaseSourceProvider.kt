package burp.ls

import burp.reporting.BurpException
import burp.reporting.PlanNode
import burp.reporting.RmlError
import burp.reporting.Origin
import burp.reporting.StatementPart
import burp.reporting.StatementParts
import burp.reporting.UnsupportedMapping
import burp.util.downloadFile
import burp.vocabularies.CSVW
import burp.vocabularies.RER
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path


fun getCompression(source: Resource): Resource =
    when (val compression = source.getPropertyResourceValue(RML.compression)) {
        null, RML.none -> RML.none
        RML.zip, RML.gzip, RML.targz, RML.tarxz -> compression
        else -> throw RuntimeException("Provided compression $compression not supported.")
    }

fun getEncoding(source: Resource): Charset =
    when (val enc = source.getPropertyResourceValue(RML.encoding)) {
        null, RML.UTF8 -> StandardCharsets.UTF_8
        RML.UTF16 -> StandardCharsets.UTF_16
        else -> throw BurpException(
            UnsupportedMapping(
                "Provided Character Set $enc not supported.",
                Origin(source.getProperty(RML.encoding), StatementPart.Predicate, StatementPart.Object)
            )
        )
    }

fun getNullValues(source: Resource): List<Any> {
    val os = mutableListOf<Any>()
    source.listProperties(RML.NULL).forEachRemaining { stmt ->
        val obj = stmt.`object`
        // WE ASSUME WE CAN HAVE RESOURCES AS NULL FOR SPARQL SOURCES
        os += if (obj.isResource) obj.asResource() else obj.asLiteral().value
    }
    return os
}


sealed interface SourceFile : PlanNode {
    fun getFile(fileOriginStmts: List<StatementParts>): File?

    data class Local(val path: String) : SourceFile {
        override fun getFile(fileOriginStmts: List<StatementParts>): File = File(path)

    }

    data class Remote(val url: String, var downloadedPath: String? = null) : SourceFile {
        override fun getFile(fileOriginStmts: List<StatementParts>): File? {
            if (downloadedPath == null) downloadedPath = downloadFile(url, this, fileOriginStmts)
            val dp = downloadedPath
            return if (dp != null) File(dp) else null
        }
    }
}

fun getFile(source: Resource, mappingDir: Path, currentWorkingDir: Path): Pair<SourceFile, List<StatementParts>> {
    if (source.hasProperty(RDF.type, RML.RelativePathSource) || source.hasProperty(RDF.type, RML.FilePath)) {
        val pathStmt = source.getProperty(RML.path)
        val file = pathStmt.literal.string
        val rootStmt = source.getProperty(RML.root)
        val root = rootStmt?.`object`
        val resolved =
            when {
                RML.MappingDirectory.equals(root) -> mappingDir.resolve(file)
                null == root || RML.CurrentWorkingDirectory.equals(root) -> currentWorkingDir.resolve(file)
                root.isLiteral -> {
                    val literal = root.asLiteral()
                    if (literal.language != null || literal.datatype != null)
                        throw RuntimeException("rml:root must be a plain literal or string $literal");

                    Path.of(literal.string).resolve(file)
                }

                else -> throw RuntimeException("RelativePathSource specified root $root is not supported.")
            }

        return SourceFile.Local(resolved.toString()) to listOf(
            StatementParts.fromPredicateObject(pathStmt),
            StatementParts.fromPredicateObject(rootStmt)
        )
    }

    if (source.hasProperty(RDF.type, DCAT.Distribution)) {
        val url = source.getPropertyResourceValue(DCAT.downloadURL).uri
        return SourceFile.Remote(url) to listOf(StatementParts.fromPredicateObject(source.getProperty(DCAT.downloadURL)))
    }

    if (source.hasProperty(RDF.type, CSVW.Table)) {
        val url = source.getProperty(CSVW.url).literal.string
        return SourceFile.Remote(url) to listOf(StatementParts.fromPredicateObject(source.getProperty(CSVW.url)))
    }


    val type = source.getPropertyResourceValue(RDF.type)
    throw BurpException(
        RmlError(
            "Source type ($type) not yet implemented in source $source",
            Origin(source.getProperty(RDF.type), StatementPart.Object),
            RER.UnsupportedMapping
        )
    )
}
