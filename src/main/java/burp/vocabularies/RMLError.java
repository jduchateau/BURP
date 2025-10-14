package burp.vocabularies;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class RMLError {
    public static final String NS = "http://w3id.org/rml/error/";
    public static final Resource ExecutionError = ResourceFactory.createResource(NS + "ExecutionError");

    public static final Resource SpecUnsupported = ResourceFactory.createResource(NS + "SpecUnsupported");
    public static final Resource OutOfSpec = ResourceFactory.createResource(NS + "OutOfSpec");
    public static final Resource UnsupportedFunction = ResourceFactory.createResource(NS + "UnsupportedFunction");
}
