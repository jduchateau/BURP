package burp.model;

import java.util.List;

import burp.reporting.BurpException;
import org.apache.jena.rdf.model.RDFNode;

import burp.model.gathermaputil.SubGraph;

public interface GatherMap {

	boolean isGatherMap();
	
	List<SubGraph> generateGatherMapGraphs(Iteration i, String baseIRI) throws BurpException;

	List<RDFNode> generateTerms(Iteration i, String baseIRI) throws BurpException;
	
}
