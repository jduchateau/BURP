package rdfobjectloader

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.vocabulary.RDF
import rdfkt.JenaDataset
import rdfkt.JenaNamedNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        val dataset = JenaDataset(model)
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
