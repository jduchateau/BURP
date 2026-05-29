package burp.model

class PredicateObjectMap : LogicalTargetScope, PlanNode {
    override val logicalTargets: MutableSet<LogicalTarget> = mutableSetOf()
    var predicateMaps = mutableListOf<PredicateMap>()
    var objectMaps = mutableListOf<ObjectMap>()
    var refObjectMaps = mutableListOf<ReferencingObjectMap>()
    var graphMaps = mutableListOf<GraphMap>()

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(predicateMaps)
        yieldAll(objectMaps)
        yieldAll(refObjectMaps)
        yieldAll(graphMaps)
    }

    override fun dependencies(): Sequence<PlanNode> = children()

    private fun unionTargets(vararg targetSets: Set<LogicalTarget>?): Set<LogicalTarget> {
        val nonNullSets = targetSets.filterNotNull().filter { it.isNotEmpty() }
        if (nonNullSets.isEmpty()) return emptySet()
        val union = mutableSetOf<LogicalTarget>()
        for (set in nonNullSets) {
            union.addAll(set)
        }
        return union
    }

    fun generate(i: Iteration): List<RdfPredicateObject> {
        val lists = mutableListOf<RdfPredicateObject>()

        for (pm in predicateMaps) {
            val predicates = pm.generateTerms(i)
            for (p in predicates) {
                if (p !is IRITerm) continue

                for (om in objectMaps) {
                    val objects = om.generateTerms(i)
                    for (o in objects) {
                        if (graphMaps.isEmpty()) {
                            lists.add(RdfPredicateObject(p, o, null, unionTargets(p.targets, o.targets)))
                        } else {
                            for (gm in graphMaps) {
                                val graphs = gm.generateTerms(i).filterIsInstance<IRITerm>()
                                for (g in graphs) {
                                    // g.targets are NOT included here — they are applied per-graph in TriplesMap
                                    // to avoid graph-level targets leaking across different named graphs
                                    lists.add(RdfPredicateObject(p, o, g, unionTargets(p.targets, o.targets)))
                                }
                            }
                        }
                    }
                }

                for (rom in refObjectMaps) {
                    val objects = rom.generateTerms(i)
                    for (o in objects) {
                        if (graphMaps.isEmpty()) {
                            lists.add(RdfPredicateObject(p, o, null, unionTargets(p.targets, o.targets)))
                        } else {
                            for (gm in graphMaps) {
                                val graphs = gm.generateTerms(i).filterIsInstance<IRITerm>()
                                for (g in graphs) {
                                    // g.targets are NOT included here — they are applied per-graph in TriplesMap
                                    lists.add(RdfPredicateObject(p, o, g, unionTargets(p.targets, o.targets)))
                                }
                            }
                        }
                    }
                }
            }
        }
        return lists
    }
}
