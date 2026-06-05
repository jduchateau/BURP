package burp.vocabularies

import org.apache.jena.rdf.model.ResourceFactory

object D2RQ {
    const val NS = "http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#"

    private fun property(local: String) = ResourceFactory.createProperty("${NS}$local")

    val jdbcDSN = property("jdbcDSN")
    val jdbcDriver = property("jdbcDriver")
    val username = property("username")
    val password = property("password")
}
