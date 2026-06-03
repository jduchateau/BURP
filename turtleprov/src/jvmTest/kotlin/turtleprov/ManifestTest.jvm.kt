package turtleprov

import org.apache.jena.riot.RDFDataMgr
import rdf.DatasetCore
import rdfkt.JenaDataset
import rdfkt.NamedTerm
import turtleprov.manifest.*
import turtleprov.manifest.gen.RdfManifest
import turtleprov.manifest.gen.RdfTest
import kotlin.test.assertIs

actual fun loadManifest(manifestPath: String): Manifest {
    val dataset = loadRdfGraphInNt(manifestPath)

    val loader = rdfobjectloader.CommonRdfObjectLoader()
    loader.addDecidableType(RdfManifest.Manifest, Manifest::class)
    loader.addDecidableType(RdfTest.TestTurtleEval, TestTurtleEval::class)
    loader.addDecidableType(RdfTest.TestTurtlePositiveSyntax, TestTurtlePositiveSyntax::class)
    loader.addDecidableType(RdfTest.TestTurtleNegativeSyntax, TestTurtleNegativeSyntax::class)
    loader.addDecidableType(RdfTest.TestTurtleNegativeEval, TestTurtleNegativeEval::class)

    registerGeneratedMappers(loader)

    val rdfType = NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    val manifestQuads = dataset.match(
        subject = null,
        predicate = rdfType,
        `object` = NamedTerm(RdfManifest.Manifest)
    )
    val manifestResource = manifestQuads.first().subject
    val manifest = loader.map(dataset, manifestResource, setOf(Manifest::class))

    manifest.includedManifest = manifest.include.associateWith { includeUri ->
        val manifestModel = RDFDataMgr.loadModel(includeUri)
        val manifestDataset = JenaDataset(manifestModel)
        val manifestQuads = manifestDataset.match(null, rdfType, NamedTerm(RdfManifest.Manifest))
        val manifest = loader.map(
            manifestDataset,
            manifestQuads.first().subject,
            setOf(Manifest::class)
        )
        manifest
    }

    return manifest
}

actual fun loadRdfGraphInNt(path: String): DatasetCore {
    val model = RDFDataMgr.loadModel(path)
    return JenaDataset(model)
}

actual fun isIsomorphic(expectedGraph: DatasetCore, actualGraph: DatasetCore): Boolean {
    assertIs<JenaDataset>(expectedGraph)
    assertIs<JenaDataset>(actualGraph)
    return expectedGraph.model.isIsomorphicWith(actualGraph.model)
}
