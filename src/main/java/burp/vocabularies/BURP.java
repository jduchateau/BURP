package burp.vocabularies;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;


public final class BURP {

	public static final String NS = "http://BURP.noname/";

	/**
	 * Synthetic annotation to find and finish processing of empty lists (after eventual concatenation) at the end.
	 */
	public static final Resource list = ResourceFactory.createResource(NS + "list");

	public static final Resource noEmpty = ResourceFactory.createResource(NS + "noEmpty");

    public static final Resource LogicalView = ResourceFactory.createProperty(NS + "LogicalView");

    public static final Resource CollectionOrContainer = ResourceFactory.createProperty(NS + "CollectionOrContainer");
}
