package burp.ls

import burp.util.Util
import burp.vocabularies.CSVW
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
        else -> throw RuntimeException("Provided Character Set $enc not supported.")
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


sealed interface SourceFile {
    fun getFile(): File?

    data class Local(val path: String) : SourceFile {
        override fun getFile(): File = File(path)

    }

    data class Remote(val url: String, var downloadedPath: String? = null) : SourceFile {
        override fun getFile(): File? {
            if (downloadedPath == null) downloadedPath = Util.downloadFile(url)
            val dp = downloadedPath
            return if (dp != null) File(dp) else null
        }
    }
}

fun getFile(source: Resource, mappingDir: Path, currentWorkingDir: Path): SourceFile {
    if (source.hasProperty(RDF.type, RML.RelativePathSource) || source.hasProperty(RDF.type, RML.FilePath)) {
        val file = source.getProperty(RML.path).literal.string
        val root = source.getPropertyResourceValue(RML.root)
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

        return SourceFile.Local(resolved.toString())
    }

    if (source.hasProperty(RDF.type, DCAT.Distribution)) {
        val url = source.getPropertyResourceValue(DCAT.downloadURL).uri
        return SourceFile.Remote(url)
    }

    if (source.hasProperty(RDF.type, CSVW.Table)) {
        val url = source.getProperty(CSVW.url).literal.string
        return SourceFile.Remote(url)
    }


    val type = source.getPropertyResourceValue(RDF.type)
    throw RuntimeException("Source type ($type) not yet implemented in source $source")
}
