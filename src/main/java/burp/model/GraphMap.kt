package burp.model;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

import java.util.List;

public class GraphMap extends TermMap {
	
	public GraphMap() {
		termType = RML.IRI;
	}

	@Override
	public String getName() {
		return "graph map";
	}

	@Override
	public List<Resource> getAllowedTermTypes() {
		return List.of(RML.IRI, RML.URI, RML.BLANKNODE);
	}

	@Override
	public boolean isGatherMap() {
		return false;
	}
	
}