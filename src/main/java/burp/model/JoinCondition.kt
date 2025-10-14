package burp.model

import org.apache.jena.rdf.model.Resource

class JoinCondition {
    lateinit var parentMap: ConcreteExpressionMap
    lateinit var childMap: ConcreteExpressionMap

    val parentValsToSubjects = mutableMapOf<String, Set<Resource>>()
}