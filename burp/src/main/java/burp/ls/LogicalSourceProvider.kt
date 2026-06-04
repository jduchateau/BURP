package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.model.Reference
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import rdfobjectloader.RDFPointer
import java.nio.file.Path

interface LogicalSourceProvider {
    fun supports(referenceFormulation: rdf.Term): Boolean
    fun create(ls: org.apache.jena.rdf.model.Resource, mappingDirectory: Path, currentWorkingDirectory: Path): LogicalSource

    /**
     * Parse a string payload into a list of nested Iterations (used by RML-LV IterableField).
     * By default, throws an exception for formulations that don't support string payloads,
     * highlighting the origin of the reference formulation that caused the issue.
     */
    fun parseStringPayload(payload: String, iterator: String?, referenceFormulationOrigin: Origin? = null): List<Iteration> {
        throw BurpException(
            RmlError(
                "Nested iterations from string payload are not supported for this reference formulation.", 
                referenceFormulationOrigin, 
                RER.UnsupportedMapping
            )
        )
    }

    /**
     * Build a Reference extractor based purely on the reference formulation.
     */
    fun buildReference(reference: String, referenceOrigin: RDFPointer, referenceFormulationOrigin: Origin? = null): Reference {
        throw BurpException(
            RmlError(
                "Nested references not supported for this formulation.", 
                referenceFormulationOrigin, 
                RER.UnsupportedMapping
            )
        )
    }
}
