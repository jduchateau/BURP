package burp.model

class PredicateObjectMap : IPlanNode {
    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null

    var predicateMaps = mutableListOf<PredicateMap>()
    var objectMaps    = mutableListOf<ObjectMap>()
    var refObjectMaps = mutableListOf<ReferencingObjectMap>()
    var graphMaps     = mutableListOf<GraphMap>()
}
