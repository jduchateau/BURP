package burp.model;

import java.util.List;

import burp.reporting.BurpException;
import burp.reporting.RmlError;
import org.apache.jena.rdf.model.RDFNode;

import burp.vocabularies.RML;

public class GraphMap extends TermMap {
	
	public GraphMap() {
		termType = RML.IRI;
	}

	public List<RDFNode> generateTerms(Iteration i, String baseIRI) throws BurpException {
		if(RML.IRI.equals(termType))
			return generateIRIs(i, baseIRI);
        if(termType == RML.URI)
            return generateURIs(i, baseIRI);
		if(RML.BLANKNODE.equals(termType))
			return generateBlankNodes(i, baseIRI);

        throw new BurpException(RmlError.Companion.IncorrectTermType("graph map", termType, List.of(RML.IRI, RML.URI, RML.BLANKNODE), this));
	}
	
	@Override
	public boolean isGatherMap() {
		return false;
	}
	
}