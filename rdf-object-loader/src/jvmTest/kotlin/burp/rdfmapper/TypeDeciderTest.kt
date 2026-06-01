package burp.rdfmapper

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import kotlin.test.Test
import rdfobjectloader.JenaRdfObjectLoader
import rdfobjectloader.TypeDecider
import rdfobjectloader.annotations.MappedByPredicate
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfType
import rdfobjectloader.annotations.RdfLiteral
import rdfobjectloader.annotations.RdfMappedFrom
import rdfobjectloader.model.DatasetCore
import rdfobjectloader.model.Term
import rdfobjectloader.JenaDatasetCore
import rdfobjectloader.JenaNamedNode
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Test Models for SPI TypeDecider
interface MyDataSource {
    var sourceFile: String
}

@RdfType("http://example.com/CSVSource")
class MyCSVSource(@RdfProperty("http://example.com/source") override var sourceFile: String) : MyDataSource

@RdfType("http://example.com/JSONSource")
class MyJSONSource(@RdfProperty("http://example.com/source") override var sourceFile: String) : MyDataSource

// SPI decider implementation (mock)
class MyDataSourceTypeDecider : TypeDecider {
    override fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>> {
        val model = (dataset as JenaDatasetCore).model
        val jenaResource = (resource as JenaNamedNode).node
        if (model.contains(
                jenaResource,
                ResourceFactory.createProperty("http://example.com/format"),
                "csv"
            )
        ) return setOf(MyCSVSource::class)
        return emptySet()
    }
}

// Test Models for MappedByPredicate (Expression style)
interface MyExpression

@MappedByPredicate("http://example.com/template")
class MyTemplate(@RdfLiteral val template: String) : MyExpression

@MappedByPredicate("http://example.com/reference")
class MyReference(@RdfLiteral val reference: String) : MyExpression

class MyExpressionMap {
    @RdfMappedFrom([MyTemplate::class, MyReference::class])
    var expression: MyExpression? = null
}

class TypeDeciderTest {

    @Test
    fun testMappedByPredicate() {
        val model = ModelFactory.createDefaultModel()
        val subject = model.createResource("http://example.com/map1")
        model.add(subject, model.createProperty("http://example.com/template"), "http://example.com/person/{id}")

        val dataset = JenaDatasetCore(model)
        val loader = JenaRdfObjectLoader()

        val result = loader.map(dataset, JenaNamedNode(subject), setOf(MyExpressionMap::class))

        val expr = result.expression
        assertTrue(expr is MyTemplate, "Expression should be mapped to MyTemplate")
        assertEquals("http://example.com/person/{id}", expr.template)
    }

    @Test
    fun testMappedByPredicateReference() {
        val model = ModelFactory.createDefaultModel()
        val subject = model.createResource("http://example.com/map2")
        model.add(subject, model.createProperty("http://example.com/reference"), "name")

        val dataset = JenaDatasetCore(model)
        val loader = JenaRdfObjectLoader()

        val result = loader.map(dataset, JenaNamedNode(subject), setOf(MyExpressionMap::class))

        val expr = result.expression
        assertTrue(expr is MyReference, "Expression should be mapped to MyReference")
        assertEquals("name", expr.reference)
    }
}



