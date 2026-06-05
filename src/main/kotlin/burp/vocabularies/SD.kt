package burp.vocabularies

import org.apache.jena.rdf.model.ResourceFactory

object SD {
    const val NS = "http://www.w3.org/ns/sparql-service-description#"

    private fun property(local: String) = ResourceFactory.createProperty("${NS}$local")
    private fun resource(local: String) = ResourceFactory.createResource("${NS}$local")

    val Service = resource("Service")

    val endpoint = property("endpoint")
}
