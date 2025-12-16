package burp.ls

import burp.model.Iteration
import burp.reporting.BurpException
import burp.reporting.Origin
import org.apache.jena.query.QueryExecution
import org.apache.jena.query.QuerySolution
import org.apache.jena.riot.RDFDataMgr
import java.util.*

class SPARQLFileSource(private val isTSV: Boolean) : FileBasedLogicalSource() {
    @Throws(BurpException::class)
    override fun iterator(): MutableIterator<Iteration?> {
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
}

internal class SPARQLIteration(sol: QuerySolution?, nulls: MutableSet<Any?>?) : Iteration(nulls) {
    private var sol: QuerySolution? = null

    init {
        this.sol = sol
    }

    override fun getValuesFor(reference: String, origin: Origin?): MutableList<Any?> {
        val l: MutableList<Any?> = ArrayList<Any?>()
        val n = sol!!.get(reference)
        if (n != null && !nulls!!.contains(n)) l.add(n)
        return l
    }

    override fun getStringsFor(reference: String, origin: Origin?): MutableList<String?> {
        val l: MutableList<String?> = ArrayList<String?>()
        val n = sol!!.get(reference)
        if (n != null && !nulls!!.contains(n)) l.add(n.toString())
        return l
    }

    override fun asString(): String? {
        throw RuntimeException("Not implemented. Does this make sense in the context of LV?")
    }
}

internal class SPARQLTSVIteration(sol: QuerySolution?, nulls: MutableSet<Any?>?) : Iteration(nulls) {
    private var sol: QuerySolution? = null

    init {
        this.sol = sol
    }

    override fun getValuesFor(reference: String, origin: Origin?): MutableList<Any?> {
        val l: MutableList<Any?> = ArrayList<Any?>()
        // REMOVE THE ? FROM THE REFERENCE
        val n = sol!!.get(reference.substring(1))
        if (n != null && !nulls!!.contains(n)) l.add(n)
        return l
    }

    override fun getStringsFor(reference: String, origin: Origin?): MutableList<String?> {
        val l: MutableList<String?> = ArrayList<String?>()
        // REMOVE THE ? FROM THE REFERENCE
        val n = sol!!.get(reference.substring(1))
        if (n != null && !nulls!!.contains(n)) l.add(n.toString())
        return l
    }

    override fun asString(): String? {
        throw RuntimeException("Not implemented. Does this make sense in the context of LV?")
    }
}