package burp.model

import burp.reporting.PlanNode
import org.apache.jena.rdf.model.Resource

abstract class Iterable : PlanNode{
    // iterator isn't defined here as we may have a default iterator, let children handle that.

    abstract var referenceFormulation: Resource
}