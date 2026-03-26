package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import org.apache.jena.query.QueryException
import org.apache.jena.query.QueryExecution
import org.apache.jena.query.QueryParseException
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP

internal class SPARQLServiceSource(private val isTSV: Boolean,
                                   override var referenceFormulation: Resource
) : LogicalSource() {
    private var iterations: MutableList<Iteration>? = null

    var iterator: String? = null
    var iteratorOrigin: Origin? = null
    var endpoint: String? = null

    @Throws(BurpException::class)
    override fun iterator(): Iterator<Iteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf<Iteration>()

                val exec: QueryExecution = QueryExecutionHTTP.service(endpoint).query(iterator).build()
                val results = exec.execSelect()

                while (results.hasNext()) {
                    val sol = results.next()

                    if (isTSV) iterations!!.add(SPARQLTSVIteration(sol, nulls))
                    else iterations!!.add(SPARQLIteration(sol, nulls))
                }
            }
            return iterations!!.iterator()
        } catch (e: QueryParseException) {
            //TODO could add line column origin
            throw BurpException(
                RmlError(
                    "SPARQL Query Parse Error in " + iterator,
                    iteratorOrigin,
                    RER.ReferenceFormulationSyntaxError,
                    e
                )
            )
        } catch (e: QueryException) {
            throw BurpException(
                RmlError(
                    "SPARQL Query Error",
                    iteratorOrigin,
                    RER.ReferenceFormulationExecutionError,
                    e
                )
            )
        } catch (e: Exception) {
            throw BurpException(RmlError("SPARQL Source Unexpected Error", iteratorOrigin, RER.LogicalSourceError, e))
        }
    }

    override fun buildReference(reference: String, origin: Origin): burp.model.Reference {
        if (isTSV) return SPARQLTSVReference(reference, origin)
        return SPARQLReference(reference, origin)
    }
}