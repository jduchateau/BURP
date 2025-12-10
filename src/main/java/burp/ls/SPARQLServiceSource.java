package burp.ls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import burp.reporting.BurpException;
import burp.reporting.Origin;
import burp.reporting.RmlError;
import burp.vocabularies.RER;
import org.apache.jena.query.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

import burp.model.Iteration;
import burp.model.LogicalSource;

class SPARQLServiceSource extends LogicalSource {

	private List<Iteration> iterations = null;
	private final boolean isTSV;

	public String iterator;
    public Origin iteratorOrigin;
	public String endpoint;

	public SPARQLServiceSource(boolean isTSV) {
		this.isTSV = isTSV;
	}

	@Override
	public Iterator<Iteration> iterator() throws BurpException {
		try {
			if (iterations == null) {
				iterations = new ArrayList<>();

				QueryExecution exec = QueryExecutionHTTP.service(endpoint).query(iterator).build();
				ResultSet results = exec.execSelect();

				while (results.hasNext()) {
					QuerySolution sol = results.next();

					if (isTSV)
						iterations.add(new SPARQLTSVIteratation(sol, nulls));
					else
						iterations.add(new SPARQLIteration(sol, nulls));
				}
			}
			return iterations.iterator();
        } catch (QueryParseException e) {
            //TODO could add line
            throw new BurpException(new RmlError("SPARQL Query Parse Error in " + iterator, iteratorOrigin, RER.ReferenceFormulationSyntaxError, e));
        } catch (QueryException e) {
            throw new BurpException(new RmlError("SPARQL Query Error", iteratorOrigin, RER.ReferenceFormulationExecutionError, e));
        } catch (Exception e) {
            throw new BurpException(new RmlError("SPARQL Source Unexpected Error", iteratorOrigin, RER.LogicalSourceError, e));
        }
	}

}