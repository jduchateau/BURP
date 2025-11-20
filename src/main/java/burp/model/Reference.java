package burp.model;

import burp.reporting.StatementParts;
import burp.reporting.Origin;

import java.util.Collections;
import java.util.List;

public class Reference extends Expression {
	
	public String reference = null;
    public List<StatementParts> origin = Collections.emptyList();
	
	public Reference(String reference) {
		this.reference = reference;
	}

	// If the term map is a reference-valued term map, 
	// then the generated RDF term is determined by applying the 
	// term generation rules to its reference value.
	public List<Object> values(Iteration i) {
		return i.getValuesFor(reference, new Origin(this, origin));
	}
	
}