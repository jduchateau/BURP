package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.model.Reference
import burp.reporting.*
import burp.vocabularies.RER
import burp.vocabularies.RML
import org.apache.jena.rdf.model.Resource
import java.nio.file.Path
import java.util.*
import kotlin.streams.asSequence

object LogicalSourceFactory {

    private val LOADER: ServiceLoader<LogicalSourceProvider> = ServiceLoader.load(LogicalSourceProvider::class.java)

    fun create(ls: Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource {
        val stmt = ls.getProperty(RML.referenceFormulation)
        val referenceFormulation = stmt.getObject().asResource()
        for (provider in LOADER) {
            if (provider.supports(referenceFormulation)) {
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
        referenceFormulation: Resource,
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
                "Reference formulation not supported for nested string iterations: $referenceFormulation",
                referenceFormulationOrigin,
                RER.UnsupportedMapping
            )
        )
    }

    fun buildReference(
        referenceFormulation: Resource, reference: String, origin: Origin, referenceFormulationOrigin: Origin? = null
    ): Reference {
        for (provider in LOADER) {
            if (provider.supports(referenceFormulation)) {
                return provider.buildReference(reference, origin, referenceFormulationOrigin)
            }
        }

        throw BurpException(
            RmlError(
                "Reference formulation not supported for nested references: $referenceFormulation",
                referenceFormulationOrigin,
                RER.UnsupportedMapping
            )
        )
    }
}