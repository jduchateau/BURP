package rdfobjectloader.burp.rdfmapper

import burp.rdfmapper.MyExpressionMap
import burp.rdfmapper.MyTemplate
import org.apache.jena.rdf.model.ModelFactory
import rdfobjectloader.JenaDatasetCore
import rdfobjectloader.JenaNamedNode
import rdfobjectloader.JenaRdfObjectLoader
import rdfobjectloader.annotations.RdfShortcutProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MyShortcutMap {
    @RdfShortcutProperty("http://example.com/shortcut", shortcutFor = "http://example.com/template")
    var nestedMap: MyExpressionMap? = null
}

class ShortcutAnnotationTest {

    @Test
    fun testRdfShortcutProperty() {
        val model = ModelFactory.createDefaultModel()
        val subject = model.createResource("http://example.com/map3")
        // We use the shortcut property directly
        model.add(subject, model.createProperty("http://example.com/shortcut"), "John Doe")

        val dataset = JenaDatasetCore(model)
        val loader = JenaRdfObjectLoader()

        val result = loader.map(dataset, JenaNamedNode(subject), setOf(MyShortcutMap::class))

        val nested = result.nestedMap
        assertTrue(nested != null, "Nested map should not be null")
        val expr = nested.expression
        assertTrue(expr is MyTemplate, "Should map the shortcut to MyTemplate via constant")
        assertEquals("John Doe", expr.template)
    }
}