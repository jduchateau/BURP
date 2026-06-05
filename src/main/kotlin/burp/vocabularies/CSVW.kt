package burp.vocabularies

import org.apache.jena.rdf.model.ResourceFactory

object CSVW {
    const val NS = "http://www.w3.org/ns/csvw#"

    private fun property(local: String) = ResourceFactory.createProperty("${NS}$local")
    private fun resource(local: String) = ResourceFactory.createResource("${NS}$local")

    val Table = resource("Table")

    val delimiter = property("delimiter")
    val dialect = property("dialect")
    val encoding = property("encoding")
    val header = property("header")
    val NULL = property("null")
    val url = property("url")
}
