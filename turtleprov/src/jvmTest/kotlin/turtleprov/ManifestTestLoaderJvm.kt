package turtleprov

import rdfobjectloader.manifest.Manifest
import rdfobjectloader.JenaRdfObjectLoader
import rdfobjectloader.JenaDatasetCore
import rdfobjectloader.JenaNamedNode
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.query.DatasetFactory
import java.io.File

actual fun loadManifest(manifestPath: String): Manifest {
    val file = File(manifestPath)
    val model = RDFDataMgr.loadModel(file.absolutePath)
    val dataset = DatasetFactory.create(model)
    val loader = JenaRdfObjectLoader()
        .addDecidableType(JenaNamedNode(ResourceFactory.createResource("http://www.w3.org/ns/rdftest#TestTurtleEval")), rdfobjectloader.manifest.TestTurtleEval::class)
        .addDecidableType(JenaNamedNode(ResourceFactory.createResource("http://www.w3.org/ns/rdftest#TestTurtleSyntax")), rdfobjectloader.manifest.TestTurtleSyntax::class)
        .addDecidableType(JenaNamedNode(ResourceFactory.createResource("http://www.w3.org/ns/rdftest#TestTurtleNegativeSyntax")), rdfobjectloader.manifest.TestTurtleNegativeSyntax::class)
        .addDecidableType(JenaNamedNode(ResourceFactory.createResource("http://www.w3.org/ns/rdftest#TestTurtleNegativeEval")), rdfobjectloader.manifest.TestTurtleNegativeEval::class)
        .addDecidableType(JenaNamedNode(ResourceFactory.createResource("http://www.w3.org/2006/03/test-description#TestCase")), rdfobjectloader.manifest.RmlTestCase::class)
    val manifestResource = model.listSubjectsWithProperty(
        org.apache.jena.vocabulary.RDF.type, 
        ResourceFactory.createResource("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#Manifest")
    ).next()
    return loader.map(JenaDatasetCore(model), JenaNamedNode(manifestResource), setOf(Manifest::class))
}

actual fun isIsomorphic(graph1Path: String, graph2Path: String): Boolean {
    val m1 = RDFDataMgr.loadModel(graph1Path)
    val m2 = RDFDataMgr.loadModel(graph2Path)
    return m1.isIsomorphicWith(m2)
}

actual fun getManifestPath(basePath: String): String {
    val basePaths = listOf(
        basePath,
        "turtleprov/$basePath",
        "../turtleprov/$basePath",
        "src/commonTest/resources/rdf-tests/rdf/rdf12/rdf-turtle"
    )
    val path = basePaths.find { java.io.File(it).exists() } ?: basePaths.last()
    return "$path/manifest.ttl"
}
