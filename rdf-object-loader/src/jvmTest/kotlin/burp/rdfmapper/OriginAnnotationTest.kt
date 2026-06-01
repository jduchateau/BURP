package rdfobjectloader.burp.rdfmapper

import org.apache.jena.rdf.model.ModelFactory
import rdf.Quad
import rdfobjectloader.*
import rdfobjectloader.annotations.OriginOfProperty
import rdfobjectloader.annotations.OriginQuad
import rdfobjectloader.annotations.RdfProperty
import kotlin.test.*

class MyOriginMap {
    @RdfProperty("http://example.com/name")
    var name: String? = null

    @OriginQuad
    var originQuad: Quad? = null
}

class MyOriginParent {
    @RdfProperty("http://example.com/child")
    var child: MyOriginMap? = null
}


class PtrOriginMap(
    @RdfProperty("http://example.com/name")
    val name: String,

    @OriginOfProperty("name")
    val nameOrigin: RDFPointer
)

class PtrOriginParent(
    @RdfProperty("http://example.com/child")
    var child: PtrOriginMap
)

class OriginTest {
    @Test
    fun `test @OriginQuad annotation with Quads`() {
        val model = ModelFactory.createDefaultModel()
        val parent = model.createResource("http://example.com/parent1")
        val child = model.createResource("http://example.com/origin1")

        // This is the quad that triggers creation of MyOriginMap
        val triggerStatement = model.createStatement(parent, model.createProperty("http://example.com/child"), child)
        model.add(triggerStatement)
        model.add(child, model.createProperty("http://example.com/name"), "Origin Test")

        val dataset = JenaDatasetCore(model)
        val loader = JenaRdfObjectLoader()

        val parentObj = loader.map(dataset, JenaNamedNode(parent), setOf(MyOriginParent::class))
        val result = parentObj.child!!

        assertEquals("Origin Test", result.name)
        assertNotNull(result.originQuad)
        val quad = result.originQuad!!
        assertEquals("http://example.com/child", quad.predicate.value)
        assertEquals("http://example.com/origin1", quad.`object`.value)
    }


    @Test
    fun `test @OriginOfRdfProperty annotation with Origin`() {
        val model = ModelFactory.createDefaultModel()
        val parent = model.createResource("http://example.com/parent1")
        val child = model.createResource("http://example.com/child1")

        // This is the quad that triggers creation of MyOriginMap
        val triggerStatement = model.createStatement(parent, model.createProperty("http://example.com/child"), child)
        model.add(triggerStatement)
        model.add(child, model.createProperty("http://example.com/name"), "Child Name")

        val dataset = JenaDatasetCore(model)
        val loader = JenaRdfObjectLoader()

        val parentObj = loader.map(dataset, JenaNamedNode(parent), setOf(PtrOriginParent::class))
        val result = parentObj.child

        assertEquals("Child Name", result.name)
        assertNotNull(result.nameOrigin)

        assertEquals("http://example.com/child1", result.nameOrigin.stmt.subject.value)
        assertEquals("http://example.com/name", result.nameOrigin.stmt.predicate.value)
        assertEquals("Child Name", result.nameOrigin.stmt.`object`.value)
        assertIs<StatementParts>(result.nameOrigin)
        assertFalse(result.nameOrigin.subject)
        assertFalse(result.nameOrigin.predicate)
        assertTrue(result.nameOrigin.`object`)
    }


}