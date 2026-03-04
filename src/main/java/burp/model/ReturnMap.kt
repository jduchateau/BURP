package burp.model;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

import java.util.List;

public class ReturnMap extends TermMap {

	public ReturnMap() {
		termType = RML.IRI;
	}

	@Override
	public String getName() {
		return "return map";
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