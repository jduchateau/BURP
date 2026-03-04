package burp.model

import burp.reporting.PlanNode
import org.apache.jena.rdf.model.Resource

interface Iterable : PlanNode{
    // iterator isn't defined here as we may have a default iterator, let children handle that.

    var referenceFormulation: Resource
}