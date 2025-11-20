package burp.model;

import burp.reporting.Origin;
import burp.reporting.PlanNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Iteration implements PlanNode {
	
	public Set<Object> nulls = new HashSet<>();
	
	public Iteration(Set<Object> nulls) {
		this.nulls = nulls;
	}

    @Deprecated
	public abstract List<Object> getValuesFor(String reference);

    public abstract List<Object> getValuesFor(String reference, Origin origin);

    @Deprecated
	public abstract List<String> getStringsFor(String reference);

    public abstract String asString();
}