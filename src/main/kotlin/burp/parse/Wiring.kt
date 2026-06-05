package burp.parse

import burp.model.*

object PlanWiring {
    fun wire(document: MappingDocument) {
        // 1. Wire parent-children tree
        wireParentChildren(document)

        // 2. Optimize mapping order
        // TODO: merge the optimization from dev-mode branche

        // 3. Rewire parent-children tree
        wireParentChildren(document)

        // 4. Wire dependencies
        //wireDependencies(document)

        // 5. Compile References
        compileReferences(document)
    }

    private fun wireParentChildren(node: PlanNode) {
        for (child in node.children()) {
            child.parent = node
            wireParentChildren(child)
        }
    }

    private fun wireDependencies(node: PlanNode) {
        // Clear dependents first if we were to rerun this
        node.dependents.clear()

        // We traverse the AST using descendants to wire dependencies
        for (descendant in node.descendants<PlanNode>()) {
            descendant.dependents.clear()
        }

        // Now we go top-down and for each node we register itself as a dependent to its dependencies
        wireDependenciesRecursive(node)
    }

    private fun wireDependenciesRecursive(node: PlanNode) {
        for (dependency in node.dependencies()) {
            dependency.dependents.add(node)
        }

        // Special case: Expressions implicitly depend on the AbstractLogicalSource of their TriplesMap
        if (node is Expression) {
            val triplesMap = node.ancestor<TriplesMap>()
            val logicalSource = triplesMap?.logicalSource
            logicalSource?.dependents?.add(node)
        }

        for (child in node.children()) {
            wireDependenciesRecursive(child)
        }
    }


    /**
     * Compiles references based on their syntactic position.
     * 
     * AST nodes implement `ReferenceHolder` (e.g., `JoinCondition`, `RawReference`) and natively compile themselves:
     * 1. `JoinCondition`s evaluate their parent Maps against the `ParentJoinReferenceScope` and child Maps against `LocalReferenceScope`.
     * 2. Standalone `RawReference`s evaluate themselves against the closest `LocalReferenceScope`.
     */
    private fun compileReferences(node: PlanNode) {
        val holders = node.descendants<ReferenceHolder>().toList()
        for (holder in holders) {
            holder.compileReferences()
        }
    }
}
