package burp.ls

import burp.model.Iteration
import burp.model.LogicalSource
import burp.model.Reference
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.ReferenceFormulationExecutionError
import burp.util.bytesToHexString
import org.apache.commons.text.StringEscapeUtils
import org.apache.jena.rdf.model.Resource
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class RDBSource() : LogicalSource() {
    var jdbcDriver: String? = null
    var jdbcDSN: String? = null
    var password: String? = null
    var username: String? = null
    var query: String? = null

    override lateinit var referenceFormulation: Resource

    @Throws(BurpException::class)
    public override fun iterator(): Iterator<Iteration> {
        try {
            val props = Properties()
            if (username != null && !username!!.isEmpty()) props.setProperty("user", username)
            if (password != null && !password!!.isEmpty()) props.setProperty("password", password)

            Class.forName(jdbcDriver)
            val connection = DriverManager.getConnection(jdbcDSN, props)
            val statement = connection.createStatement()
            val resultset = statement.executeQuery(query)

            val indexMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()
            for (i in 1..resultset.getMetaData().getColumnCount()) {
                indexMap.put(resultset.getMetaData().getColumnLabel(i), i)
            }

            return object : Iterator<Iteration> {
                override fun hasNext(): Boolean {
                    try {
                        val goNext = resultset.next()
                        if (!goNext) {
                            resultset.close()
                            statement.close()
                            connection.close()
                        }
                        return goNext
                    } catch (e: SQLException) {
                        throw BurpException(
                            ReferenceFormulationExecutionError(
                                "Problem querying database while iterating over rows.",
                                this@RDBSource
                            )
                        )
                    }
                }

                override fun next(): Iteration {
                    return RDBIteration(resultset, indexMap, nulls)
                }
            }
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    override fun sourceReference(reference: String, origin: Origin) = RDBReference(reference, origin)
}

class RDBReference(reference: String?, origin: Origin) : burp.model.Reference(reference, origin) {
    override fun getValues(i: Iteration): List<Any?> {
        require(i is RDBIteration)
        val l: MutableList<Any?> = ArrayList<Any?>()
        val columnname = StringEscapeUtils.unescapeJava(reference)

        if (!i.values.containsKey(columnname) && !i.values.containsKey(
                columnname?.replace(
                    "\"",
                    ""
                )
            )
        ) throw RuntimeException("Attribute $columnname does not exist.")

        var value = i.values.get(columnname)


        // Check whether the user added the right column names in the mappings
        if (value == null)  // Now try without quotes
            value = i.values.get(columnname?.replace("\"", ""))

        if (value != null && !i.nulls.contains(value)) l.add(value)

        return l
    }
}

internal class RDBIteration(resultSet: ResultSet, indexMap: MutableMap<String?, Int?>, nulls: MutableSet<Any?>) :
    Iteration(nulls) {
    val values: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()

    init {
        for (ref in indexMap.keys) {
            try {
                var o = resultSet.getObject(indexMap.get(ref)!!)
                if (o != null) {
                    if (o is ByteArray) {
                        o = bytesToHexString(o)
                    }
                }

                values.put(ref, o)
            } catch (e: Exception) {
                throw RuntimeException("Error retrieving values from result set.")
            }
        }
    }



    override fun asString(): String? {
        throw UnsupportedOperationException("Not implemented. Does this make sense in the context of LV?")
    }
}