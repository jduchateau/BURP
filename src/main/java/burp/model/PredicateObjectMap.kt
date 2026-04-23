package burp.model

class PredicateObjectMap : PlanNode {
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

    fun generate(i: Iteration, baseIRI: String): List<RdfPredicateObject> {
        val lists = mutableListOf<RdfPredicateObject>()
        val predicates = predicateMaps.flatMap { it.generateTerms(i) }

        val objects = mutableListOf<Term>()
        for (om in objectMaps) objects.addAll(om.generateTerms(i))
        for (rom in refObjectMaps) objects.addAll(rom.generateTerms(i))

        val predicateObjectGraphs =
            graphMaps.flatMap { it.generateTerms(i).filterIsInstance<IRITerm>() }.toSet()
        val graphs = if (graphMaps.isEmpty()) setOf(null) else predicateObjectGraphs

        for (p in predicates) {
            if (p !is IRITerm) continue
            for (o in objects) {
                for (g in graphs) {
                    lists.add(RdfPredicateObject(p, o, g))
                }
            }
        }
        return lists
    }
}
