package burp.util

import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.SourceAccessError
import burp.vocabularies.RML
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

fun getDecompressedFile(file: String, compression: Resource?, fileTrace: Origin): String {
    try {
        if (RML.none == compression) return file

        val temp = Files.createTempFile(null, ".extracted.tmp").toString()

        val out: OutputStream = FileOutputStream(temp)
        val fin = FileInputStream(file)
        var inputStream: InputStream? = null

        if (RML.zip == compression) {
            val a = ZipInputStream(fin)
            a.getNextEntry()
            inputStream = a
        } else if (RML.gzip == compression) {
            inputStream = GzipCompressorInputStream(fin)
        } else if (RML.targz == compression) {
            val a = TarArchiveInputStream(GzipCompressorInputStream(fin))
            a.getNextEntry()
            inputStream = a
        } else if (RML.tarxz == compression) {
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