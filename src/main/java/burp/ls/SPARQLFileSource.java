package burp.ls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

import burp.model.Iteration;
import org.jetbrains.annotations.NotNull;

public class SPARQLFileSource extends FileBasedLogicalSource {

	private final boolean isTSV;

	public SPARQLFileSource(boolean isTSV) {
		this.isTSV = isTSV;
	}

    private List<Iteration> iterations = null;

	@Override
	public Iterator<Iteration> iterator() {
		try {
			if (iterations == null) {
				iterations = new ArrayList<>();

				Dataset ds = RDFDataMgr.loadDataset(file);

				try (QueryExecution exec = QueryExecution.dataset(ds).query(iterator).build()) {
					ResultSet results = exec.execSelect();
					
					while(results.hasNext()) {
						QuerySolution sol = results.next();
						
						if(isTSV)
							iterations.add(new SPARQLTSVIteration(sol, nulls));
						else
							iterations.add(new SPARQLIteration(sol, nulls));
					}
				}
			}
			return iterations.iterator();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}

class SPARQLIteration extends Iteration {

	private QuerySolution sol = null;
	
	protected SPARQLIteration(QuerySolution sol, Set<Object> nulls) {
		super(nulls);

		this.sol = sol;
	}

	@Override
	public List<Object> getValuesFor(String reference) {
		List<Object> l = new ArrayList<>();
		RDFNode n = sol.get(reference);
		if(n != null && !nulls.contains(n))
			l.add(n);
		return l;
	}

	@Override
	public List<String> getStringsFor(String reference) {
		List<String> l = new ArrayList<>();
		RDFNode n = sol.get(reference);
		if(n != null && !nulls.contains(n))
			l.add(n.toString());
		return l;
	}
	
}

class SPARQLTSVIteration extends Iteration {

	private QuerySolution sol = null;
	
	protected SPARQLTSVIteration(QuerySolution sol, Set<Object> nulls) {
		super(nulls);

		this.sol = sol;
	}

	@Override
	public List<Object> getValuesFor(String reference) {
		List<Object> l = new ArrayList<>();
		// REMOVE THE ? FROM THE REFERENCE
		RDFNode n = sol.get(reference.substring(1));
		if(n != null && !nulls.contains(n))
			l.add(n);
		return l;
	}

	@NotNull
    @Override
	public List<String> getStringsFor(String reference) {
		List<String> l = new ArrayList<>();
		// REMOVE THE ? FROM THE REFERENCE
		RDFNode n = sol.get(reference.substring(1));
		if(n != null && !nulls.contains(n))
			l.add(n.toString());
		return l;
	}
	
}