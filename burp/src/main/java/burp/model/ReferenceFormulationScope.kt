package burp.model

import rdfobjectloader.RDFPointer

interface LocalReferenceScope : PlanNode {
    /**
     * Builds a reference for the local scope context.
     * Use this for normal properties, subjects, objects, or the childMap of a join condition.
     */
    fun buildLocalReference(reference: String, origin: RDFPointer): Reference
}

interface ExportedReferenceScope : PlanNode {
    /**
     * Builds an exported reference for external querying.
     * When a [TriplesMap] uses a `LogicalView` as its logical source, references from the TriplesMap
     * are evaluated against the exported fields of the LogicalView, not its internal iterators.
     */
    fun buildExportedReference(reference: String, origin: RDFPointer): Reference
}

interface ParentJoinReferenceScope : PlanNode {
    /**
     * Builds a reference for a joined parent scope.
     * When a [JoinCondition] evaluates its `parentMap`, it asks its scope to build a parent reference.
     */
    fun buildParentJoinReference(reference: String, origin: RDFPointer): Reference
}

interface ReferenceHolder : PlanNode {
    fun compileReferences()
}