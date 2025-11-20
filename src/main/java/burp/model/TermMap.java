package burp.model;

import burp.model.gathermaputil.GatherMapMixin;
import burp.model.gathermaputil.SubGraph;
import burp.reporting.BurpException;
import burp.reporting.PlanNode;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

public abstract class TermMap extends ExpressionMap implements GatherMap, PlanNode {

	public DatatypeMap datatypeMap = null;
	public LanguageMap languageMap = null;
	public Resource termType;

	public GatherMapMixin gatherMap = null;
	
	@Override
	public List<SubGraph> generateGatherMapGraphs(Iteration i, String baseIRI) throws BurpException {
		if(!isGatherMap())
			throw new IllegalStateException("Trying to process a non-gathermap as gathermap");
		
		List<SubGraph> g = new ArrayList<>();
		
		if(expression == null) {
            g.addAll(gatherMap.generateGraphs(i, baseIRI));
		} else {
			for(RDFNode n : generateTerms(i, baseIRI)) {
				for(SubGraph sg : gatherMap.generateGraphs(i, baseIRI)) {
					sg.updateNode(n);
					g.add(sg);
				}
			}
		}
		
		return g;
	}

}