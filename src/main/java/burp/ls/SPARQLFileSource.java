package burp.ls;

import java.util.*;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

import burp.model.Iteration;

public class SPARQLFileSource extends FileBasedLogicalSource {

	private final boolean isTSV;

	public SPARQLFileSource(boolean isTSV) {
		this.isTSV = isTSV;
	}

	@Override
	public Iterator<Iteration> iterator() {
		try {
			if (iterations == null) {
				iterations = new ArrayList<>();

				Dataset ds = RDFDataMgr.loadDataset(Objects.requireNonNull(file.getFile()).getPath());

				try (QueryExecution exec = QueryExecution.dataset(ds).query(iterator).build()) {
					ResultSet results = exec.execSelect();
					
					while(results.hasNext()) {
						QuerySolution sol = results.next();
						
						if(isTSV)
							iterations.add(new SPARQLTSVIteratation(sol, nulls));
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

    @Override
    public String asString() {
        throw new RuntimeException("Not implemented. Does this make sense in the context of LV?");
    }

}

class SPARQLTSVIteratation extends Iteration {

	private QuerySolution sol = null;
	
	protected SPARQLTSVIteratation(QuerySolution sol, Set<Object> nulls) {
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

	@Override
	public List<String> getStringsFor(String reference) {
		List<String> l = new ArrayList<>();
		// REMOVE THE ? FROM THE REFERENCE
		RDFNode n = sol.get(reference.substring(1));
		if(n != null && !nulls.contains(n))
			l.add(n.toString());
		return l;
	}

    @Override
    public String asString() {
        throw new RuntimeException("Not implemented. Does this make sense in the context of LV?");
    }
}