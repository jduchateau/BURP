package burp.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;

import burp.vocabularies.RML;

public class SubjectMap extends TermMap {

	public List<Resource> classes = new ArrayList<>();
	public List<GraphMap> graphMaps = new ArrayList<>();
	
	public SubjectMap() {
		termType = RML.IRI;
	}

	@Override
	public String getName() {
		return "subject map";
	}

	@Override
	public List<Resource> getAllowedTermTypes() {
		return List.of(RML.IRI, RML.URI, RML.BLANKNODE);
	}

	@Override
	public boolean isGatherMap() {
		return gatherMap != null;
	}

}