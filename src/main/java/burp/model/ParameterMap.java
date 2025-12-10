package burp.model;

import java.util.List;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

public class ParameterMap extends TermMap {

	public ParameterMap() {
		termType = RML.IRI;
	}

	@Override
	public String getName() {
		return "parameter map";
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