package burp.model;

import burp.model.gathermap.SubGraph;
import burp.reporting.BurpException;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;

public interface GatherMap {

	boolean isGatherMap();
	
	List<SubGraph> generateGatherMapGraphs(Iteration i, String baseIRI) throws BurpException;

	List<RDFNode> generateTerms(Iteration i, String baseIRI) throws BurpException;
	
}
