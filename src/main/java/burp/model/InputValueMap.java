package burp.model;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

import java.util.List;

public class InputValueMap extends TermMap {
	
	public DatatypeMap datatypeMap = null;
	public LanguageMap languageMap = null;
	
	public InputValueMap() {
		termType = RML.LITERAL;
	}

	@Override
	public String getName() {
		return "input value map";
	}

	@Override
	public List<Resource> getAllowedTermTypes() {
		return List.of(RML.IRI, RML.URI, RML.BLANKNODE, RML.LITERAL);
	}

	@Override
	public boolean isGatherMap() {
		return false;
	}
	
}