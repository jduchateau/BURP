package burp.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import burp.model.gathermaputil.GatherMapMixin;
import burp.model.gathermaputil.SubGraph;
import org.jetbrains.annotations.Nullable;

public abstract class TermMap extends ExpressionMap implements GatherMap {

	public Resource termType;

	public GatherMapMixin gatherMap = null;

    public TermMap(@Nullable Expression expression) {
        super(expression);
    }

    @Override
	public List<SubGraph> generateGatherMapGraphs(Iteration i, String baseIRI) {
        assert isGatherMap() : "Trying to process a non-gathermap as gathermap";
		
		List<SubGraph> g = new ArrayList<SubGraph>();
		
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