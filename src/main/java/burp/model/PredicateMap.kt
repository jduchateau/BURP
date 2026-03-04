package burp.model;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

import java.util.List;

public class PredicateMap extends TermMap {

	public PredicateMap() {
		termType = RML.IRI;
	}

	@Override
	public String getName() {
		return "predicate map";
	}

	@Override
	public List<Resource> getAllowedTermTypes() {
		return List.of(RML.IRI, RML.URI);
	}

	@Override
	public boolean isGatherMap() {
		return false;
	}
	
}