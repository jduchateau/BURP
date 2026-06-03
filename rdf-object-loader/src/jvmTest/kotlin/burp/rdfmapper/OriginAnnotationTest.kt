package rdfobjectloader.burp.rdfmapper

import org.apache.jena.rdf.model.ModelFactory
import rdfkt.JenaDataset
import rdfkt.JenaNamedNode
import rdfobjectloader.JenaRdfObjectLoader
import rdfobjectloader.RDFPointer
import rdfobjectloader.StatementParts
import rdfobjectloader.annotations.OriginOfProperty
import rdfobjectloader.annotations.RdfProperty
import kotlin.test.*

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
    fun `test @OriginOfRdfProperty annotation with Origin`() {
        val model = ModelFactory.createDefaultModel()
        val parent = model.createResource("http://example.com/parent1")
        val child = model.createResource("http://example.com/child1")

        // This is the quad that triggers creation of MyOriginMap
        val triggerStatement = model.createStatement(parent, model.createProperty("http://example.com/child"), child)
        model.add(triggerStatement)
        model.add(child, model.createProperty("http://example.com/name"), "Child Name")

        val dataset = JenaDataset(model)
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