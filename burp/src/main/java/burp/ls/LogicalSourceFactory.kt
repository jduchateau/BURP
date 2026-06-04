package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.model.Reference
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.reporting.UnsupportedMapping
import burp.vocabularies.RER
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource
import rdfobjectloader.RDFPointer
import rdfobjectloader.StatementPart
import java.nio.file.Path
import java.util.*
import kotlin.streams.asSequence

object LogicalSourceFactory {

    private val LOADER: ServiceLoader<LogicalSourceProvider> = ServiceLoader.load(LogicalSourceProvider::class.java)

    fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val stmt = ls.getProperty(RML.referenceFormulation)
        val referenceFormulation = stmt.getObject().asResource()
        val referenceFormulationTerm = rdfkt.NamedTerm(referenceFormulation.uri)
        for (provider in LOADER) {
            if (provider.supports(referenceFormulationTerm)) {
                return provider.create(ls, mappingDirectory, currentWorkingDirectory)
            }
        }

        val supported = LOADER.stream().asSequence().joinToString(", ") { it.type().name }
        val supportedMessage = if (supported.isNotEmpty()) "Are supported: $supported."
        else "None are supported, provide a `burp.ls.LogicalSourceProvider` in class path."

        throw BurpException(
            UnsupportedMapping(
                "Reference formulation not supported: $referenceFormulation. $supportedMessage",
                Origin(stmt, StatementPart.Object)
            )
        )
    }

    fun changeIterator(
        iterationAsString: String,
        referenceFormulation: rdf.Term,
        iterator: String?,
        referenceFormulationOrigin: Origin? = null
    ): List<Iteration> {
        for (provider in LOADER) {
            if (provider.supports(referenceFormulation)) {
                return provider.parseStringPayload(iterationAsString, iterator, referenceFormulationOrigin)
            }
        }

        throw BurpException(
            RmlError(
                "Reference formulation not supported for nested string iterations: ${referenceFormulation.value}",
                referenceFormulationOrigin,
                RER.UnsupportedMapping
            )
        )
    }

    fun buildReference(
        referenceFormulation: rdf.Term, reference: String, referenceOrigin: RDFPointer, referenceFormulationOrigin: Origin? = null
    ): Reference {
        for (provider in LOADER) {
            if (provider.supports(referenceFormulation)) {
                return provider.buildReference(reference, referenceOrigin, referenceFormulationOrigin)
            }
        }

        throw BurpException(
            RmlError(
                "Reference formulation not supported for nested references: ${referenceFormulation.value}",
                referenceFormulationOrigin,
                RER.UnsupportedMapping
            )
        )
    }
}