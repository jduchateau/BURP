package rdfobjectloader

import rdfkt.InMemoryDatasetCore
import rdfkt.Literal
import rdfkt.NamedTerm
import rdfobjectloader.annotations.*
import kotlin.test.*

interface MyExpression

@MappedByPredicate("http://example.com/template")
data class MyTemplate(
    @RdfLiteral val template: String
) : MyExpression

@MappedByPredicate("http://example.com/reference")
data class MyReference(
    @RdfLiteral val reference: String
) : MyExpression

data class MyExpressionMap(
    @RdfMappedFrom([MyTemplate::class, MyReference::class])
    val expression: List<MyExpression>
)

data class MyShortcutMap(
    @RdfProperty("http://example.com/expression")
    @RdfShortcutProperty("http://example.com/shortcut", shortcutFor = "http://example.com/template")
    val nestedMap: MyExpressionMap?
)

class ManualMyTemplateMapper : RdfModelMapper<MyTemplate> {
    override fun map(
        dataset: rdf.DatasetCore,
        resource: rdf.Term,
        loader: RdfObjectLoader,
        cache: MutableMap<rdf.Term, Any>
    ): MyTemplate {
        val template = MyTemplate(resource.value)
        cache[resource] = template
        return template
    }
}

class ManualMyReferenceMapper : RdfModelMapper<MyReference> {
    override fun map(
        dataset: rdf.DatasetCore,
        resource: rdf.Term,
        loader: RdfObjectLoader,
        cache: MutableMap<rdf.Term, Any>
    ): MyReference {
        val reference = MyReference(resource.value)
        cache[resource] = reference
        return reference
    }
}

class ManualMyExpressionMapMapper : RdfModelMapper<MyExpressionMap> {
    override fun map(
        dataset: rdf.DatasetCore,
        resource: rdf.Term,
        loader: RdfObjectLoader,
        cache: MutableMap<rdf.Term, Any>
    ): MyExpressionMap {
        val list = mutableListOf<MyExpression>()

        val templateQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://example.com/template")).toList()
        for (quad in templateQuads) {
            val expr = loader.map(dataset, quad.`object`, setOf(MyTemplate::class))
            list.add(expr)
        }

        val referenceQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://example.com/reference")).toList()
        for (quad in referenceQuads) {
            val expr = loader.map(dataset, quad.`object`, setOf(MyReference::class))
            list.add(expr)
        }

        val mapObj = MyExpressionMap(list)
        cache[resource] = mapObj
        return mapObj
    }
}

class ManualMyShortcutMapMapper : RdfModelMapper<MyShortcutMap> {
    override fun map(
        dataset: rdf.DatasetCore,
        resource: rdf.Term,
        loader: RdfObjectLoader,
        cache: MutableMap<rdf.Term, Any>
    ): MyShortcutMap {
        val expressionsList = mutableListOf<MyExpression>()

        // 1. Process shortcut property (http://example.com/shortcut) if present
        val shortcutQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://example.com/shortcut")).toList()
        for (quad in shortcutQuads) {
            val virtualSubject = rdfkt.BlankTerm("virtual_bnode_shortcut_${quad.`object`.value}")

            val virtualDataset = rdfkt.InMemoryDatasetCore()
            for (q in dataset) {
                val s = mapRdfTermToRdfkt(q.subject) as rdfkt.BlankNodeOrIRI
                val p = mapRdfTermToRdfkt(q.predicate) as NamedTerm
                val o = mapRdfTermToRdfkt(q.`object`)
                val g = mapRdfTermToRdfkt(q.graph) as rdfkt.Graph
                virtualDataset.add(rdfkt.Quad(s, p, o, g))
            }

            val mappedObject = mapRdfTermToRdfkt(quad.`object`)
            virtualDataset.add(rdfkt.Quad(virtualSubject, NamedTerm("http://example.com/template"), mappedObject))

            val nested = loader.map(virtualDataset, virtualSubject, setOf(MyExpressionMap::class))
            expressionsList.addAll(nested.expression)
        }

        // 2. Process standard property (http://example.com/expression) if present
        val expressionQuads =
            dataset.match(subject = resource, predicate = NamedTerm("http://example.com/expression")).toList()
        for (quad in expressionQuads) {
            val exprNode = quad.`object`
            val nested = loader.map(dataset, exprNode, setOf(MyExpressionMap::class))
            expressionsList.addAll(nested.expression)
        }

        if (shortcutQuads.isNotEmpty() || expressionQuads.isNotEmpty()) {
            val nestedMap = MyExpressionMap(expressionsList)
            val mapObj = MyShortcutMap(nestedMap)
            cache[resource] = mapObj
            return mapObj
        }

        val mapObj = MyShortcutMap(null)
        cache[resource] = mapObj
        return mapObj
    }
}

class ExpressionTest {
    @Test
    fun testMapSimpleEntity() {
        val dataset = InMemoryDatasetCore()
        val personTerm = NamedTerm("http://example.com/person/1")
        val addressTerm = NamedTerm("http://example.com/address/1")

        dataset.add(
            rdfkt.Quad(
                personTerm,
                NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NamedTerm("http://schema.org/Person")
            )
        )
        dataset.add(rdfkt.Quad(personTerm, NamedTerm("http://schema.org/name"), Literal("Alice")))
        dataset.add(rdfkt.Quad(personTerm, NamedTerm("http://schema.org/age"), Literal("30")))
        dataset.add(rdfkt.Quad(personTerm, NamedTerm("http://schema.org/email"), Literal("alice@example.com")))
        dataset.add(rdfkt.Quad(personTerm, NamedTerm("http://schema.org/email"), Literal("alice.work@example.com")))
        dataset.add(rdfkt.Quad(personTerm, NamedTerm("http://schema.org/address"), addressTerm))

        dataset.add(
            rdfkt.Quad(
                addressTerm,
                NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NamedTerm("http://schema.org/PostalAddress")
            )
        )
        dataset.add(rdfkt.Quad(addressTerm, NamedTerm("http://schema.org/streetAddress"), Literal("123 Main St")))
        dataset.add(rdfkt.Quad(addressTerm, NamedTerm("http://schema.org/addressLocality"), Literal("Wonderland")))

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
    fun testMappedByPredicate() {
        val dataset = InMemoryDatasetCore()
        val subject = NamedTerm("http://example.com/map1")
        dataset.add(
            rdfkt.Quad(
                subject,
                NamedTerm("http://example.com/template"),
                Literal("http://example.com/person/{id}")
            )
        )

        val loader = CommonRdfObjectLoader()
        registerGeneratedMappers(loader)

        val result = loader.map(dataset, subject, setOf(MyExpressionMap::class))
        assertEquals(1, result.expression.size)
        val expr = result.expression.first()
        assertTrue(expr is MyTemplate, "Expression should be mapped to MyTemplate")
        assertEquals("http://example.com/person/{id}", expr.template)
    }

    @Test
    fun testMappedByPredicateMultiple() {
        val dataset = InMemoryDatasetCore()
        val subject = NamedTerm("http://example.com/map1")
        dataset.add(
            rdfkt.Quad(
                subject,
                NamedTerm("http://example.com/template"),
                Literal("http://example.com/person/{id}")
            )
        )
        dataset.add(rdfkt.Quad(subject, NamedTerm("http://example.com/reference"), Literal("name")))

        val loader = CommonRdfObjectLoader()
        registerGeneratedMappers(loader)

        val result = loader.map(dataset, subject, setOf(MyExpressionMap::class))
        assertEquals(2, result.expression.size)
        assertTrue(result.expression[0] is MyTemplate)
        assertEquals("http://example.com/person/{id}", (result.expression[0] as MyTemplate).template)
        assertTrue(result.expression[1] is MyReference)
        assertEquals("name", (result.expression[1] as MyReference).reference)
    }

    @Test
    fun testRdfShortcutProperty() {
        val dataset = InMemoryDatasetCore()
        val subject = NamedTerm("http://example.com/map3")
        val templateValue = "MyTemplateValue"
        dataset.add(rdfkt.Quad(subject, NamedTerm("http://example.com/shortcut"), Literal(templateValue)))

        val loader = CommonRdfObjectLoader()
        registerGeneratedMappers(loader)

        val result = loader.map(dataset, subject, setOf(MyShortcutMap::class))
        assertNotNull(result.nestedMap)
        assertEquals(1, result.nestedMap.expression.size)
        val expr = result.nestedMap.expression.first()
        assertIs<MyTemplate>(expr)
        assertEquals(templateValue, expr.template)
    }

    @Test
    fun testRdfMappedFromWithShortcutPropertyUnused() {
        val dataset = InMemoryDatasetCore()
        val map3 = NamedTerm("http://example.com/map3")
        val expression3 = NamedTerm("http://example.com/expression3")
        dataset.add(rdfkt.Quad(map3, NamedTerm("http://example.com/expression"), expression3))
        val reference = "Reference"
        dataset.add(rdfkt.Quad(expression3, NamedTerm("http://example.com/reference"), Literal(reference)))

        val loader = CommonRdfObjectLoader()
        registerGeneratedMappers(loader)

        val result = loader.map(dataset, map3, setOf(MyShortcutMap::class))
        assertNotNull(result.nestedMap)
        assertEquals(1, result.nestedMap.expression.size)
        val expr = result.nestedMap.expression.first()
        assertIs<MyReference>(expr)
        assertEquals(reference, expr.reference)
    }

    @Test
    fun testShortcutAndFullPropertyCombined() {
        val dataset = InMemoryDatasetCore()
        val map3 = NamedTerm("http://example.com/map3")
        val expression3 = NamedTerm("http://example.com/expression3")

        // 1. Shortcut property
        dataset.add(rdfkt.Quad(map3, NamedTerm("http://example.com/shortcut"), Literal("John Doe")))

        // 2. Explicit expression property
        dataset.add(rdfkt.Quad(map3, NamedTerm("http://example.com/expression"), expression3))
        dataset.add(rdfkt.Quad(expression3, NamedTerm("http://example.com/reference"), Literal("Reference")))

        val loader = CommonRdfObjectLoader()
        registerGeneratedMappers(loader)

        val result = loader.map(dataset, map3, setOf(MyShortcutMap::class))
        assertNotNull(result.nestedMap)
        assertEquals(2, result.nestedMap.expression.size)

        val firstExpr = result.nestedMap.expression[0]
        assertTrue(firstExpr is MyTemplate)
        assertEquals("John Doe", firstExpr.template)

        val secondExpr = result.nestedMap.expression[1]
        assertTrue(secondExpr is MyReference)
        assertEquals("Reference", secondExpr.reference)
    }
}
