package burp.model

interface Iterable : PlanNode {
    // iterator isn't defined here as we may have a default iterator, let children handle that.

    var referenceFormulation: rdf.Term
}