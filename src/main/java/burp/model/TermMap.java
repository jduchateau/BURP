package burp.model;

import burp.model.gathermap.GatherMapMixin;
import burp.model.gathermap.SubGraph;
import burp.reporting.BurpException;
import burp.reporting.ErrorsKt;
import burp.reporting.PlanNode;
import burp.vocabularies.RML;
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


    public abstract String getName();

    public abstract List<Resource> getAllowedTermTypes();

    @Override
    public List<RDFNode> generateTerms(Iteration i, String baseIRI) throws BurpException {
        List<Resource> allowed = getAllowedTermTypes();

        if (RML.IRI.equals(termType) && allowed.contains(RML.IRI))
            return generateIRIs(i, baseIRI);
        if (RML.URI.equals(termType) && allowed.contains(RML.URI))
            return generateURIs(i, baseIRI);
        if (RML.BLANKNODE.equals(termType) && allowed.contains(RML.BLANKNODE))
            return generateBlankNodes(i, baseIRI);
        if (RML.LITERAL.equals(termType) && allowed.contains(RML.LITERAL))
            return generateLiterals(i, baseIRI, datatypeMap, languageMap);

        throw new BurpException(ErrorsKt.IncorrectTermType(getName(), termType,
                getAllowedTermTypes(), this));
    }

}