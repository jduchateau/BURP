package burp.util

import burp.vocabularies.RML
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.jena.rdf.model.Resource
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private fun writeTarEntry(file: File, tos: TarArchiveOutputStream, suffixes: Array<String>, writeAction: (OutputStream) -> Unit) {
    val entryName = suffixes.fold(file.name) { name, suffix -> name.removeSuffix(suffix) }
    val entry = TarArchiveEntry(entryName)

    val baos = java.io.ByteArrayOutputStream()
    writeAction(baos)
    val bytes = baos.toByteArray()

    entry.size = bytes.size.toLong()
    tos.putArchiveEntry(entry)
    tos.write(bytes)
    tos.closeArchiveEntry()
}

fun writeCompressedFile(file: File, compression: Resource?, writeAction: (OutputStream) -> Unit) {
    if (compression == null || compression == RML.none) {
        FileOutputStream(file).use { out ->
            writeAction(out)
        }
        return
    }

    FileOutputStream(file).use { fos ->
        when (compression) {
            RML.zip -> {
                ZipOutputStream(fos).use { zos ->
                    val entryName = file.name.removeSuffix(".zip")
                    zos.putNextEntry(ZipEntry(entryName))
                    writeAction(zos)
                    zos.closeEntry()
                }
            }
            RML.gzip -> {
                GzipCompressorOutputStream(fos).use { gos ->
                    writeAction(gos)
                }
            }
            RML.targz -> {
                GzipCompressorOutputStream(fos).use { gos ->
                    TarArchiveOutputStream(gos).use { tos ->
                        writeTarEntry(file, tos, arrayOf(".tar.gz", ".tgz"), writeAction)
                    }
                }
            }
            RML.tarxz -> {
                XZCompressorOutputStream(fos).use { xos ->
                    TarArchiveOutputStream(xos).use { tos ->
                        writeTarEntry(file, tos, arrayOf(".tar.xz"), writeAction)
                    }
                }
            }
            else -> {
                throw RuntimeException("Provided compression $compression not supported.")
            }
        }
    }
}
