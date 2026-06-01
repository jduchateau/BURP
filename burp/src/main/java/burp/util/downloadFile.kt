package burp.util

import burp.ls.SourceFile
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.SourceAccessError
import rdfobjectloader.StatementParts
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

fun downloadFile(url: String, file: SourceFile?, fileOriginStmts: List<StatementParts>?): String {
    try {
        val temp = Files.createTempFile(null, ".download.tmp").toString()

        val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
        val response = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
            .send<InputStream?>(request, HttpResponse.BodyHandlers.ofInputStream())
        val output = FileOutputStream(temp)
        output.write(response.body()!!.readAllBytes())
        output.close()

        return temp
    } catch (ex: Exception) {
        throw BurpException(
            SourceAccessError(
                "Problem downloading $url",
                Origin(planNode = file, sourceStatements = fileOriginStmts),
                ex
            )
        )
    }
}