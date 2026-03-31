package burp.ls

import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.Origin
import org.apache.jena.query.QueryExecution
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.RDFDataMgr
import java.util.*

class SPARQLFileSource(private val isTSV: Boolean,
                       override var referenceFormulation: Resource
) : FileBasedLogicalSource() {
    var iterator: String? = null
    var iteratorOrigin: Origin? = null

    @Throws(BurpException::class)
    override fun iterator(): Iterator<Iteration> {
        try {
            if (iterations == null) {
                iterations = mutableListOf()

                val filePath = Objects.requireNonNull(file?.getFile(fileOriginStmts))!!.path
                val ds = RDFDataMgr.loadDataset(filePath)

                QueryExecution.dataset(ds).query(iterator).build().use { exec ->
                    val results = exec.execSelect()
                    while (results.hasNext()) {
                        val sol = results.next()

                        if (isTSV) iterations!!.add(SPARQLTSVIteration(sol, nulls))
                        else iterations!!.add(SPARQLIteration(sol, nulls))
                    }
                }
            }
            return iterations!!.iterator()
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    override fun sourceReference(reference: String, origin: Origin): burp.model.Reference {
        if (isTSV) return SPARQLTSVReference(reference, origin)
        return SPARQLReference(reference, origin)
    }
}

class SPARQLReference(reference: String?, origin: Origin) : burp.model.Reference(reference, origin) {
    override fun getValues(i: Iteration): List<Any?> {
        require(i is SPARQLIteration)
        val l: MutableList<Any?> = ArrayList()
        val n = i.sol?.get(reference)
        if (n != null && !i.nulls.contains(n)) l.add(n)
        return l
    }
}

class SPARQLTSVReference(reference: String?, origin: Origin) : burp.model.Reference(reference, origin) {
    override fun getValues(i: Iteration): List<Any?> {
        require(i is SPARQLTSVIteration)
        val l = mutableListOf<Any?>()
        // REMOVE THE ? FROM THE REFERENCE
        val n = i.sol?.get(reference?.substring(1))
        if (n != null && !i.nulls.contains(n)) l.add(n)
        return l
    }
}

internal class SPARQLIteration(sol: QuerySolution?, nulls: MutableSet<Any?>) : Iteration(nulls) {
    var sol: QuerySolution? = null

    init {
        this.sol = sol
    }


    override fun asString(): String? {
        throw RuntimeException("Not implemented. Does this make sense in the context of LV?")
    }
}

internal class SPARQLTSVIteration(sol: QuerySolution?, nulls: MutableSet<Any?>) : Iteration(nulls) {
    var sol: QuerySolution? = null

    init {
        this.sol = sol
    }


    override fun asString(): String? {
        throw RuntimeException("Not implemented. Does this make sense in the context of LV?")
    }
}