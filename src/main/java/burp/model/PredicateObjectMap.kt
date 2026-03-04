package burp.model

class PredicateObjectMap {
    var predicateMaps = mutableListOf<PredicateMap>()
    var objectMaps = mutableListOf<ObjectMap>()
    var refObjectMaps = mutableListOf<ReferencingObjectMap>()
    var graphMaps = mutableListOf<GraphMap>()
}
