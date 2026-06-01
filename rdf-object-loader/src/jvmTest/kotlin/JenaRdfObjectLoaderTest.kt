package rdfobjectloader

import rdfobjectloader.annotations.RdfId
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfType
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.vocabulary.RDF
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RdfType("http://schema.org/Person")
data class Person(
    @RdfId val id: String,
    @RdfProperty("http://schema.org/name") val name: String,
    @RdfProperty("http://schema.org/age") val age: Int,
    @RdfProperty("http://schema.org/address") val address: PostalAddress?,
    @RdfProperty("http://schema.org/email") val emails: List<String>
)

@RdfType("http://schema.org/PostalAddress")
data class PostalAddress(
    @RdfProperty("http://schema.org/streetAddress") val street: String,
    @RdfProperty("http://schema.org/addressLocality") val city: String
)

class JenaRdfObjectLoaderTest {
    @Test
    fun testMapSimpleEntity() {
        val model = ModelFactory.createDefaultModel()
        val personRes = model.createResource("http://example.com/person/1")
        val addressRes = model.createResource("http://example.com/address/1")
        
        personRes.addProperty(RDF.type, model.createResource("http://schema.org/Person"))
        personRes.addProperty(model.createProperty("http://schema.org/name"), "Alice")
        personRes.addLiteral(model.createProperty("http://schema.org/age"), 30)
        personRes.addProperty(model.createProperty("http://schema.org/email"), "alice@example.com")
        personRes.addProperty(model.createProperty("http://schema.org/email"), "alice.work@example.com")
        personRes.addProperty(model.createProperty("http://schema.org/address"), addressRes)
        
        addressRes.addProperty(RDF.type, model.createResource("http://schema.org/PostalAddress"))
        addressRes.addProperty(model.createProperty("http://schema.org/streetAddress"), "123 Main St")
        addressRes.addProperty(model.createProperty("http://schema.org/addressLocality"), "Wonderland")

        val dataset = JenaDatasetCore(model)
        val mapper = JenaRdfObjectLoader()

        val person = mapper.map(dataset, JenaNamedNode(personRes), setOf(Person::class))

        assertNotNull(person)
        assertEquals("http://example.com/person/1", person.id)
        assertEquals("Alice", person.name)
        assertEquals(30, person.age)
        assertEquals(2, person.emails.size)
        assertTrue(person.emails.contains("alice@example.com"))
        assertNotNull(person.address)
        assertEquals("123 Main St", person.address!!.street)
        assertEquals("Wonderland", person.address!!.city)
    }
}
