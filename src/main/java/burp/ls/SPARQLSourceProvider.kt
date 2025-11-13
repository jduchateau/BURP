package burp.ls

import burp.model.LogicalSource
import burp.util.Util
import burp.vocabularies.RML
import burp.vocabularies.SD
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VOID
import java.net.MalformedURLException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath


@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
class SPARQLSourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: Resource): Boolean =
        RML.SPARQL_Results_CSV.equals(referenceFormulation) || RML.SPARQL_Results_TSV.equals(referenceFormulation) || RML.SPARQL_Results_XML.equals(
            referenceFormulation
        ) || RML.SPARQL_Results_JSON.equals(referenceFormulation)

    // TODO: Do we really need RML.SPARQL_Results_TSV?
    // TODO: Do we really need RML.SPARQL_Results_JSON?
    // TODO: Do we really need RML.SPARQL_Results_XML?
    override fun create(
        ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path
    ): LogicalSource {
        val iterator = ls.getProperty(RML.iterator).literal.string
        val sourceNode = ls.getPropertyResourceValue(RML.source)
        val isTSV = RML.SPARQL_Results_TSV.equals(sourceNode.getPropertyResourceValue(RDF.type))

        if (sourceNode.hasProperty(RDF.type, VOID.Dataset)) {
            val source = SPARQLFileSource(isTSV)
            val file = sourceNode.getPropertyResourceValue(VOID.dataDump).uri
            source.file = getAbsoluteOrRelativeFromFileProtocol(file, currentWorkingDirectory)
            source.compression = getCompression(sourceNode)
            source.encoding = getEncoding(sourceNode)
            source.iterator = iterator
            source.nulls.addAll(getNullValues(sourceNode))
            return source
        } else if (sourceNode.hasProperty(RDF.type, SD.Service)) {
            val source = SPARQLServiceSource(isTSV)
            source.endpoint = sourceNode.getPropertyResourceValue(SD.endpoint).uri
            source.iterator = iterator
            source.nulls.addAll(getNullValues(sourceNode))
            return source
        } else {
            // WE HAVE A SIMPLE SPARQL SOURCE
            val source = SPARQLFileSource(isTSV)
            source.file = getFile(sourceNode, mappingDirectory, currentWorkingDirectory)
            source.compression = getCompression(sourceNode)
            source.encoding = getEncoding(sourceNode)
            source.iterator = iterator
            source.nulls.addAll(getNullValues(sourceNode))
            return source
        }
    }


    private fun getAbsoluteOrRelativeFromFileProtocol(fileUri: String, rootPath: Path): SourceFile? {
        try {
            val url = URI(fileUri);
            if (Util.isAbsoluteAndValidIRI(fileUri)) return SourceFile.Remote(fileUri)
            if (url.scheme.equals("file")) {
                return if (url.toPath().isAbsolute) SourceFile.Local(url.path)
                else SourceFile.Local(rootPath.resolve(url.path).toString())
            }
            return null
        } catch (e: MalformedURLException) {
            throw RuntimeException("$fileUri is not a file URL.", e);
        }
    }
}

