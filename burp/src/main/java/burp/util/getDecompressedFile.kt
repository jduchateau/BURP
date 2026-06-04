package burp.util

import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.SourceAccessError
import burp.vocabularies.MyRml
import burp.vocabularies.Rml
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.IOUtils
import org.apache.jena.rdf.model.Resource
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

fun getDecompressedFile(file: String, compression: Resource?, fileTrace: Origin?): String {
    try {
        val compressionUri = compression?.uri
        if (compressionUri == null || Rml.none == compressionUri) return file

        val originalName = java.io.File(file).name
        val innerExtension = when (compressionUri) {
            Rml.zip -> originalName.removeSuffix(".zip")
            Rml.gzip -> originalName.removeSuffix(".gz")
            MyRml.targz, MyRml.targzip -> originalName.removeSuffix(".tar.gz").removeSuffix(".tgz")
            Rml.tarxz -> originalName.removeSuffix(".tar.xz")
            else -> originalName
        }
        val suffix = if (innerExtension.contains(".")) "." + innerExtension.substringAfterLast(".") else ".extracted.tmp"

        val temp = Files.createTempFile(null, suffix).toString()

        val out: OutputStream = FileOutputStream(temp)
        val fin = FileInputStream(file)
        var inputStream: InputStream? = null

        if (Rml.zip == compressionUri) {
            val a = ZipInputStream(fin)
            a.getNextEntry()
            inputStream = a
        } else if (Rml.gzip == compressionUri) {
            inputStream = GzipCompressorInputStream(fin)
        } else if (MyRml.targz == compressionUri || MyRml.targzip == compressionUri) {
            val a = TarArchiveInputStream(GzipCompressorInputStream(fin))
            a.getNextEntry()
            inputStream = a
        } else if (Rml.tarxz == compressionUri) {
            val a = TarArchiveInputStream(XZCompressorInputStream(fin))
            a.getNextEntry()
            inputStream = a
        }

        IOUtils.copy(inputStream, out)
        inputStream!!.close()
        out.close()

        return temp
    } catch (e: Exception) {
        System.err.println(compression)
        throw BurpException(
            SourceAccessError(
                "Error decompressing file $file with $compression",
                fileTrace,
                e
            )
        )
    }
}