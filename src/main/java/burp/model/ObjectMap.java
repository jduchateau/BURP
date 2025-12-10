package burp.model;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

import java.util.List;

public class ObjectMap extends TermMap {
	
	public ObjectMap() {
		termType = RML.IRI;
	}

	@Override
	public String getName() {
		return "object map";
	}

	@Override
	public List<Resource> getAllowedTermTypes() {
		return List.of(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL);
	}

	@Override
	public boolean isGatherMap() {
		return gatherMap != null;
	}
	
}