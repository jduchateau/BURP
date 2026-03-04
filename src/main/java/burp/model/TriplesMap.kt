package burp.model

import org.apache.jena.rdf.model.Resource

class TriplesMap(var subject: Resource?) {
    var logicalSource: AbstractLogicalSource? = null
    lateinit var subjectMap: SubjectMap
    var predicateObjectMaps= mutableListOf<PredicateObjectMap>()
    var baseIRI: String? = null


    var countGeneratedStatements: Long = 0
}