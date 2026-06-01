package burp.ls

import burp.model.LogicalSource
import burp.reporting.Origin
import burp.util.isValidAndAbsoluteIRI
import burp.vocabularies.RML
import burp.vocabularies.SD
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VOID
import rdfobjectloader.JenaQuad
import rdfobjectloader.StatementPart
import rdfobjectloader.StatementParts
import java.net.MalformedURLException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath


@Suppress("unused")
@AutoService(LogicalSourceProvider::class)
class SPARQLSourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: Resource): Boolean =
        RML.SPARQL_Results_CSV.equals(referenceFormulation)
                || RML.SPARQL_Results_TSV.equals(referenceFormulation)
                || RML.SPARQL_Results_XML.equals(referenceFormulation)
                || RML.SPARQL_Results_JSON.equals(referenceFormulation)

    override fun create(
        ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path
    ): LogicalSource {
        val iterator = ls.getProperty(RML.iterator).literal.string
        val iteratorOrigin = Origin(ls.getProperty(RML.iterator), StatementPart.Object)
        val sourceNode = ls.getPropertyResourceValue(RML.source)
        val isTSV = RML.SPARQL_Results_TSV.equals(sourceNode.getPropertyResourceValue(RDF.type))
        val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)

        if (sourceNode.hasProperty(RDF.type, VOID.Dataset)) {
            val source = SPARQLFileSource(isTSV, referenceFormulation)
            val file = sourceNode.getPropertyResourceValue(VOID.dataDump).uri
            source.file = getAbsoluteOrRelativeFromFileProtocol(file, currentWorkingDirectory)
            source.fileOriginStmts = listOf(StatementParts.fromPredicateObject(JenaQuad(sourceNode.getProperty(VOID.dataDump))))
            source.compression = getCompression(sourceNode)
            source.encoding = getEncoding(sourceNode)
            source.iterator = iterator
            source.iteratorOrigin = iteratorOrigin
            source.nulls.addAll(getNullValues(sourceNode))
            return source
        } else if (sourceNode.hasProperty(RDF.type, SD.Service)) {
            val source = SPARQLServiceSource(isTSV, referenceFormulation)
            source.endpoint = sourceNode.getPropertyResourceValue(SD.endpoint).uri
            source.iterator = iterator
            source.iteratorOrigin = iteratorOrigin
            source.nulls.addAll(getNullValues(sourceNode))
            return source
        } else {
            // WE HAVE A SIMPLE SPARQL SOURCE
            val source = SPARQLFileSource(isTSV, referenceFormulation)
            val (file, origin) = getFile(sourceNode, mappingDirectory, currentWorkingDirectory)
            source.file = (file)
            source.fileOriginStmts = origin
            source.compression = getCompression(sourceNode)
            source.encoding = getEncoding(sourceNode)
            source.iterator = iterator
            source.iteratorOrigin = iteratorOrigin
            source.nulls.addAll(getNullValues(sourceNode))
            return source
        }
    }


    private fun getAbsoluteOrRelativeFromFileProtocol(fileUri: String, rootPath: Path): SourceFile? {
        try {
            val url = URI(fileUri);
            if (isValidAndAbsoluteIRI(fileUri)) return SourceFile.Remote(fileUri)
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

