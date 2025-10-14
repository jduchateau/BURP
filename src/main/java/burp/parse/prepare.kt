package burp.parse

import burp.model.PredicateObjectMap
import burp.model.ReferencingObjectMap
import burp.model.TriplesMap


/** Initialize [TriplesMap.parentOf] and  */
fun initializeTriplesMapForIndexedJoins(tms: List<TriplesMap>) {
    val parentTriplesMap = mutableMapOf<TriplesMap, MutableList<ReferencingObjectMap>>()
    for (tm in tms) {
        for (pom in tm.predicateObjectMaps) {
            pom.parent = tm
            for (rom in pom.refObjectMaps) {
                rom.parent = pom
                val parent = rom.parentTriplesMap
                if (parent !in parentTriplesMap) parentTriplesMap[parent] = mutableListOf()
                parentTriplesMap[parent]!!.add(rom)
            }
        }
    }
    for ((k, v) in parentTriplesMap) k.parentOf = v.toSet()
}

/**
 * Prepare triples maps for **IndexedJoins** generation
 *
 *  It will reorder [TriplesMap] in a Topological Order
 *  and may split [TriplesMap] to break cycles, in which case the full [TriplesMap] is separated
 *  from its problematic [PredicateObjectMap] (with the [ReferencingObjectMap] causing the cycle)
 *  in a separate [TriplesMap] that is run later.
 */
fun prepareForIndexedJoins(tms: List<TriplesMap>): List<TriplesMap> {
    // Ensure [TriplesMap.parentOf] is initialized
    initializeTriplesMapForIndexedJoins(tms)

    // So we have
    // TriplesMap (equal)
    //  + PredicateObjectMap
    //     + ReferencingObjectMap
    //        + parent TriplesMap (may form cycles)
    //        + child TriplesMap (equal)

    val result = mutableListOf<TriplesMap>()
    val visited = mutableSetOf<TriplesMap>()
    val onStack = ArrayDeque<TriplesMap>()

    fun dfs(triplesMap: TriplesMap) {
        var tm = triplesMap
        if (tm in visited) return
        if (tm in onStack) {
            // Cycle detected
            // Break the first item in the cycle
            // a b C d e f C into a b C d e f C_bis
            // where C_bis contains the problematic ReferencingObjects which is also removed from C.
            val problematicReferencingObjectMap = tm
                .predicateObjectMaps.flatMap { it.refObjectMaps }
                .filter { rom -> rom.parentTriplesMap in onStack }
                .toSet()
            if (problematicReferencingObjectMap.isNotEmpty()) {

                val newTM = TriplesMap(tm.iri)
                newTM.logicalSource = tm.logicalSource
                newTM.subjectMap = tm.subjectMap
                newTM.predicateObjectMaps = tm.predicateObjectMaps.map { pom ->
                    val newPOM = PredicateObjectMap()
                    newPOM.parent = newTM
                    newPOM.graphMaps = pom.graphMaps
                    newPOM.predicateMaps = pom.predicateMaps
                    newPOM.refObjectMaps = pom.refObjectMaps.filter { it in problematicReferencingObjectMap }.toMutableList()
                    for (rom in newPOM.refObjectMaps) {
                        rom.parent = newPOM
                        // No need to change parentOf in parent triples map as we do move the rom and not copy it.
                        // So parent triples map still points to the right place.
                    }
                    newPOM
                }.filter { pom -> pom.refObjectMaps.isNotEmpty() }

                tm.predicateObjectMaps = tm.predicateObjectMaps.map { pom ->
                    val fixPOM = PredicateObjectMap()
                    fixPOM.parent = tm
                    fixPOM.graphMaps = pom.graphMaps
                    fixPOM.predicateMaps = pom.predicateMaps
                    fixPOM.objectMaps = pom.objectMaps
                    fixPOM.refObjectMaps = pom.refObjectMaps.filter { it !in problematicReferencingObjectMap }.toMutableList()
                    for (rom in fixPOM.refObjectMaps) {
                        rom.parent = fixPOM
                    }
                    fixPOM
                }.filter { pom -> pom.refObjectMaps.isNotEmpty() || pom.objectMaps.isNotEmpty() }

                // Do as if we were analyzing the newTM instead
                tm = newTM
            }
        }
        onStack.add(tm)
        for (dep in tm.parentOf) {
            val parentTM = dep.ancestorOfType(TriplesMap::class)
            dfs(parentTM!!)
        }
        onStack.remove(tm)
        visited.add(tm)
        result.add(tm)
    }

    for (tm in tms) {
        dfs(tm)
    }

    return result.reversed()
}
