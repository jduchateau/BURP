package rdfobjectloader

import rdf.DatasetCore
import rdf.Term
import rdfkt.*
import rdfobjectloader.annotations.OriginOfProperty
import rdfobjectloader.annotations.RdfId
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfType
import kotlin.test.*

@RdfType("http://schema.org/Person")
class Person(
    @RdfId val id: String,
    @RdfProperty("http://schema.org/name") val name: String,
    @OriginOfProperty("age") val originAge: RDFPointer?,
    @RdfProperty("http://schema.org/age") val age: Int
) {
    @OriginOfProperty("address")
    var originAddress: RDFPointer? = null

    @RdfProperty("http://schema.org/address")
    var address: PostalAddress? = null

    @RdfProperty("http://schema.org/email")
    var emails: List<String> = emptyList()

    @RdfProperty("http://schema.org/knows")
    var knows: List<Person> = emptyList()
}

@RdfType("http://schema.org/PostalAddress")
data class PostalAddress(
    @RdfProperty("http://schema.org/streetAddress") val street: String,
    @RdfProperty("http://schema.org/addressLocality") val city: String
)

class ManualPostalAddressMapper : RdfModelMapper<PostalAddress> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): PostalAddress {
        val streetQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/streetAddress")).toList()
        val street = streetQuads.firstOrNull()?.`object`?.value ?: ""

        val cityQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/addressLocality")).toList()
        val city = cityQuads.firstOrNull()?.`object`?.value ?: ""

        val address = PostalAddress(street, city)
        cache[resource] = address
        return address
    }
}


class ManualPersonMapper : RdfModelMapper<Person> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): Person {
        val id = resource.value

        val nameQuads = dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/name")).toList()
        val name = nameQuads.firstOrNull()?.`object`?.value ?: ""

        val ageQuads = dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/age")).toList()
        val ageQuad = ageQuads.firstOrNull()
        val age = ageQuad?.`object`?.value?.toIntOrNull() ?: 0
        val originAge = ageQuad?.let { StatementParts.fromObject(it) }

        // Instantiate the object with constructor params
        val person = Person(id, name, originAge, age)
        cache[resource] = person

        // Map mutable properties
        val emailQuads = dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/email")).toList()
        person.emails = emailQuads.map { it.`object`.value }

        val addressQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/address")).toList()
        val addressQuad = addressQuads.firstOrNull()
        val addressRes = addressQuad?.`object`
        person.address = addressRes?.let {
            loader.map(dataset, it, setOf(PostalAddress::class))
        }
        person.originAddress = addressQuad?.let { StatementParts.fromObject(it) }

        // Map recursive cycle knows list
        val knowsQuads = dataset.match(subject = resource, predicate = NamedTerm("http://schema.org/knows")).toList()
        person.knows = knowsQuads.map { quad ->
            loader.map(dataset, quad.`object`, setOf(Person::class))
        }

        return person
    }
}

class PersonTest {
    @Test
    fun `test mapping Person with Address`() {
        val dataset = InMemoryDatasetCore()
        val personTerm = NamedTerm("http://example.com/person/1")
        val addressTerm = NamedTerm("http://example.com/address/1")

        dataset.add(
            Quad(
                personTerm,
                NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NamedTerm("http://schema.org/Person")
            )
        )
        dataset.add(Quad(personTerm, NamedTerm("http://schema.org/name"), Literal("Alice")))
        dataset.add(Quad(personTerm, NamedTerm("http://schema.org/age"), Literal("30")))
        dataset.add(Quad(personTerm, NamedTerm("http://schema.org/email"), Literal("alice@example.com")))
        dataset.add(Quad(personTerm, NamedTerm("http://schema.org/email"), Literal("alice.work@example.com")))
        dataset.add(Quad(personTerm, NamedTerm("http://schema.org/address"), addressTerm))

        dataset.add(
            Quad(
                addressTerm,
                NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NamedTerm("http://schema.org/PostalAddress")
            )
        )
        dataset.add(Quad(addressTerm, NamedTerm("http://schema.org/streetAddress"), Literal("123 Main St")))
        dataset.add(Quad(addressTerm, NamedTerm("http://schema.org/addressLocality"), Literal("Wonderland")))

        val loader = CommonRdfObjectLoader()
        loader.addDecidableType(NamedTerm("http://schema.org/Person"), Person::class)
        loader.addDecidableType(NamedTerm("http://schema.org/PostalAddress"), PostalAddress::class)

        registerGeneratedMappers(loader)

        val person = loader.map(dataset, personTerm, setOf(Person::class))

        assertNotNull(person)
        assertEquals("http://example.com/person/1", person.id)
        assertEquals("Alice", person.name)
        assertEquals(30, person.age)
        assertEquals(2, person.emails.size)
        assertTrue(person.emails.contains("alice@example.com"))
        assertNotNull(person.address)
        assertEquals("123 Main St", person.address!!.street)
        assertEquals("Wonderland", person.address!!.city)

        // Assert OriginOfProperty works
        assertNotNull(person.originAge)
        assertEquals("http://schema.org/age", person.originAge.stmt.predicate.value)
        assertEquals("30", person.originAge.stmt.`object`.value)

        assertNotNull(person.originAddress)
        assertEquals("http://schema.org/address", person.originAddress!!.stmt.predicate.value)
        assertEquals("http://example.com/address/1", person.originAddress!!.stmt.`object`.value)
    }

    @Test
    fun `test mapping Person with Cycles`() {
        val dataset = InMemoryDatasetCore()
        val person1 = NamedTerm("http://example.com/person/1")
        val person2 = NamedTerm("http://example.com/person/2")

        dataset.add(
            Quad(
                person1,
                NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NamedTerm("http://schema.org/Person")
            )
        )
        dataset.add(Quad(person1, NamedTerm("http://schema.org/name"), Literal("Alice")))
        dataset.add(Quad(person1, NamedTerm("http://schema.org/age"), Literal("30", XSD.integer)))
        dataset.add(Quad(person1, NamedTerm("http://schema.org/knows"), person2))

        dataset.add(
            Quad(
                person2,
                NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NamedTerm("http://schema.org/Person")
            )
        )
        dataset.add(Quad(person2, NamedTerm("http://schema.org/name"), Literal("Bob")))
        dataset.add(Quad(person2, NamedTerm("http://schema.org/age"), Literal("32")))
        dataset.add(Quad(person2, NamedTerm("http://schema.org/knows"), person1))

        val loader = CommonRdfObjectLoader()
        loader.addDecidableType(NamedTerm("http://schema.org/Person"), Person::class)
        registerGeneratedMappers(loader)

        val alice = loader.map(dataset, person1, setOf(Person::class))

        assertNotNull(alice)
        assertEquals("Alice", alice.name)
        assertEquals(1, alice.knows.size)

        val bob = alice.knows.first()
        assertEquals("Bob", bob.name)
        assertEquals(1, bob.knows.size)

        // Assert cycle resolution: Bob's friend points back to the exact same Alice instance
        assertSame(bob.knows.first(), alice, "Cycle was not resolved to the same instance")
    }
}
