package burp.model

import rdf.RDF

class MappingDocument(val triplesMaps: List<TriplesMap>) : PlanNode {
    override var parent: PlanNode? = null

    override fun children(): Sequence<PlanNode> = triplesMaps.asSequence()
    override fun dependencies(): Sequence<PlanNode> = triplesMaps.asSequence()

    fun generate(): List<RdfStatement> {
        val stmts = mutableListOf<RdfStatementLike>()
        for (tm in triplesMaps) {
            val logicalSource =
                tm.logicalSource ?: throw RuntimeException("Constant Triples Map without logical source")
            val iter = logicalSource.iterator()
            while (iter.hasNext()) {
                val i = iter.next()
                stmts.addAll(tm.generate(i))
            }
        }

        return postProcessContainers(stmts)
    }

    private fun postProcessContainers(stmts: List<RdfStatementLike>): List<RdfStatement> {
        // Collect all distinct CollectionOrContainerTerms used as an actual subject or object or contained in other collections
        // While keeping them separate by graphs, when the id was generated
        val containers = mutableMapOf<GraphId, MutableMap<Term, CollectionOrContainerTerm>>()
        val expandedContainer = mutableMapOf<GraphId, MutableSet<CollectionOrContainerTerm>>()
        val result = mutableListOf<RdfStatement>()

        fun extractAndMergeContainers(t: Term, graph: GraphId, replaceTermBy: (CollectionOrContainerTerm) -> Unit) {
            if (t is CollectionOrContainerTerm
                // is it a container that we did not expand previously (new container)
                && expandedContainer.getOrPut(graph) { mutableSetOf() }.add(t)
            ) {
                val graphContainers = containers.getOrPut(graph) { mutableMapOf() }
                if (graphContainers.containsKey(t.id)) {
                    // Merge elements and point to the merged collection
                    graphContainers[t.id]!!.elements.addAll(t.elements)
                    replaceTermBy(graphContainers[t.id]!!)
                } else {
                    graphContainers[t.id] = t
                }
                t.elements.forEach { extractAndMergeContainers(it, graph, replaceTermBy) }
            }
        }

        for (stmt in stmts) {
            when (stmt) {
                is RdfStatement -> {
                    extractAndMergeContainers(stmt.subject, stmt.graph) { stmt.subject = it }
                    extractAndMergeContainers(stmt.`object`, stmt.graph) { stmt.`object` = it }
                }

                is RdfStatementSubjectGraph -> {
                    extractAndMergeContainers(stmt.subject, stmt.graph) { stmt.subject = it }
                }
            }
        }

        // Change the generated id in different graphs
        for ((graph, graphContainers) in containers.entries) {
            graphContainers.values.forEachIndexed { index, c ->
                if (c.id is BlankNodeTerm) {
                    val newId = BlankNodeTerm("gathermap-${graph ?: "default"}-${index}")
                    graphContainers[c.id]!!.id = newId
                }
            }
        }

        // Unroll the containers into raw RdfStatements based on their type
        val containerStmts = mutableListOf<RdfStatement>()

        for ((graph, graphContainers) in containers.entries) {
            for (c in graphContainers.values) {
                val subject = c.id

                fun emitContainerStmts(type: IRITerm) {
                    containerStmts.add(RdfStatement(subject, RDF.type.iriTerm, type))
                    c.elements.forEachIndexed { i, element ->
                        containerStmts.add(
                            RdfStatement(subject, RDF.underscore(i + 1).iriTerm, element.itselfOrId(), graph)
                        )
                    }
                }

                when (c) {
                    is RdfListTerm -> {
                        // Turn Lists into rdf:first / rdf:rest chains
                        var currentListId = subject
                        for (i in c.elements.indices) {
                            val isLast = i == c.elements.size - 1
                            val element = c.elements[i]
                            val firstStmt = RdfStatement(currentListId, RDF.first.iriTerm, element.itselfOrId(), graph)
                            containerStmts.add(firstStmt)

                            val restObj = if (isLast) RDF.nil.iriTerm else BlankNodeTerm("${subject}_${i + 1}")
                            val restStmt = RdfStatement(currentListId, RDF.rest.iriTerm, restObj, graph)
                            containerStmts.add(restStmt)

                            currentListId = restObj
                        }
                        // Empty list -> replace the actual term references below with rdf:nil
                    }

                    is RdfBagTerm -> emitContainerStmts(RDF.Bag.iriTerm)
                    is RdfSeqTerm -> emitContainerStmts(RDF.Seq.iriTerm)
                    is RdfAltTerm -> emitContainerStmts(RDF.Alt.iriTerm)
                }
            }
        }

        // Rewrite object/subject references from CollectionOrContainerTerm to BlankNodeTerm (id) or nil for empty lists
        fun rewrite(t: Term): Term {
            if (t is RdfListTerm && t.elements.isEmpty()) return RDF.nil.iriTerm
            if (t is CollectionOrContainerTerm) return t.id
            return t
        }

        val rdfStmtsWithoutCollections = stmts.filterIsInstance<RdfStatement>()
            // Of collections used in stmts we just keep the id.
            .map { RdfStatement(rewrite(it.subject) as BlankNodeOrIRI, it.predicate, rewrite(it.`object`), it.graph) }
        result.addAll(rdfStmtsWithoutCollections)
        result.addAll(containerStmts)

        return result
    }
}

fun Term.itselfOrId() = if (this is CollectionOrContainerTerm) id else this
