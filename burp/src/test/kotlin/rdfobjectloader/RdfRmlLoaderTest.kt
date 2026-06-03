package rdfobjectloader

import burp.model.Template
import burp.model.TriplesMap
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileUtils
import rdfkt.JenaDataset
import rdfkt.JenaNamedNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class RdfRmlLoaderTest {

    @Test
    fun testLoadTriplesMap() {
        val model = ModelFactory.createDefaultModel()
        val inputStream = RdfRmlLoaderTest::class.java.getResourceAsStream("/rdfobjectloader/tm+sm.ttl")
        assertNotNull(inputStream, "Could not load mapping.ttl")
        model.read(inputStream, "http://example.com/base/", FileUtils.langTurtle)

        val dataset = JenaDataset(model)
        val mapper = JenaRdfObjectLoader()

        val triplesMapRes = model.getResource("http://example.com/TriplesMap1")
        val triplesMap = mapper.map(dataset, JenaNamedNode(triplesMapRes), setOf(TriplesMap::class))

        assertNotNull(triplesMap)
        assertEquals("http://example.com/TriplesMap1", triplesMap.subject?.uri)

        assertNotNull(triplesMap.subjectMap)
        assertEquals(1, triplesMap.subjectMap.classes.size)
        assertEquals("http://example.com/Person", triplesMap.subjectMap.classes[0].uri)

        assertNotNull(triplesMap.subjectMap.expression)
        assertIs<Template>(triplesMap.subjectMap.expression)
        assertEquals("http://example.com/person/{id}", (triplesMap.subjectMap.expression as Template).template)
    }
}
