package burp.vocabularies

import org.apache.jena.rdf.model.ResourceFactory

object BURP {

    val NS = "http://BURP.noname/";
    val base_uri = NS;

    private fun property(local: String) = ResourceFactory.createProperty("${NS}$local")
    private fun resource(local: String) = ResourceFactory.createResource("${NS}$local")

    /**
     * Synthetic annotation to find and finish processing of empty lists (after eventual concatenation) at the end.
     */
    val list = property("list");

    val noEmpty = property("noEmpty");

    val LogicalView = resource("LogicalView");

    val CollectionOrContainer = resource("CollectionOrContainer");

    /**
     * Annotations of turtle tokens provenance when converting turtleprov parser results into an RDF 1.2 model.
     */
    val TOKEN = property("token")

    val FILE_ID = property("file")
    val START_LINE = property("startLine")
    val START_COLUMN = property("startColumn")
    val END_LINE = property("endLine")
    val END_COLUMN = property("endColumn")

    val STRING_START_LINE = property("startLine")
    val STRING_START_COLUMN = property("startColumn")
    val STRING_END_LINE = property("endLine")
    val STRING_END_COLUMN = property("endColumn")

    val BLANK_NODE_ID = property("blankNodeId")

    val SUBJECT = resource("SubjectProv")
    val PREDICATE = resource("PredicateProv")
    val OBJECT = resource("ObjectProv")

    // For testing manifest
    val TestTurtleAnnotation = resource("TestTurtleAnnotation")
}