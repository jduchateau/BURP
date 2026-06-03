package turtleprov

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import rdf.DatasetCore
import turtleprov.manifest.*
import turtleprov.manifest.gen.RdfManifest
import turtleprov.manifest.gen.RdfTest


actual fun loadManifest(manifestPath: String): Manifest {
    val dataset = loadRdfGraphInNt(manifestPath)

    val loader = rdfobjectloader.CommonRdfObjectLoader()
    loader.addDecidableType(RdfManifest.Manifest, Manifest::class)
    loader.addDecidableType(RdfTest.TestTurtleEval, TestTurtleEval::class)
    loader.addDecidableType(RdfTest.TestTurtlePositiveSyntax, TestTurtlePositiveSyntax::class)
    loader.addDecidableType(RdfTest.TestTurtleNegativeSyntax, TestTurtleNegativeSyntax::class)
    loader.addDecidableType(RdfTest.TestTurtleNegativeEval, TestTurtleNegativeEval::class)

    registerGeneratedMappers(loader)

    val manifestQuads = dataset.match(
        subject = null,
        predicate = rdfkt.NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
        `object` = rdfkt.NamedTerm(RdfManifest.Manifest)
    )
    val manifestResource = manifestQuads.first().subject

    val manifest = loader.map(dataset, manifestResource, setOf(Manifest::class))

    val includedMap = mutableMapOf<String, Manifest>()
    for (include in manifest.include) {
        val subManifest = loadManifest(include)
        includedMap[include] = subManifest
    }
    manifest.includedManifest = includedMap

    return manifest
}

actual fun loadRdfGraphInNt(path: String): DatasetCore {
    val n3 = js("require('n3')")
    val absolutePath = if (path.startsWith("file://")) {
        path.removePrefix("file://")
    } else if (!path.startsWith("/")) {
        "/home/jakub/Documents/Dev/BURP/$path"
    } else {
        path
    }
    val rawContent = SystemFileSystem.source(Path(absolutePath)).buffered().readString()
    @Suppress("unused") val baseIRI = "file://$absolutePath"
    val parser = js("new n3.Parser({ baseIRI: baseIRI, version: '1.2' })")
    val quads = parser.parse(rawContent)
    val store = js("new n3.Store()")
    store.addQuads(quads)
    return rdfkt.N3Dataset(store)
}

actual fun isIsomorphic(expectedGraph: DatasetCore, actualGraph: DatasetCore): Boolean {
    val n3 = js("require('n3')")
    val rdfIsomorphic = js("require('rdf-isomorphic')")

    val q1 = expectedGraph.toJsQuads(n3)
    val q2 = actualGraph.toJsQuads(n3)

    val isomorphicFn = rdfIsomorphic.isomorphic
    return isomorphicFn(q1, q2) as Boolean
}

private fun DatasetCore.toJsQuads(n3: dynamic): Array<dynamic> {
    if (this is rdfkt.N3Dataset) {
        return this.store.getQuads(null, null, null, null) as Array<dynamic>
    }
    val factory = n3.DataFactory
    val quadsList = mutableListOf<dynamic>()
    for (q in this) {
        fun toJsTerm(t: rdf.Term): dynamic {
            return when (t.termType) {
                "NamedNode" -> factory.namedNode(t.value)
                "BlankNode" -> factory.blankNode(t.value)
                "Literal" -> {
                    val lit = t as rdf.Literal
                    if (lit.language.isNotEmpty()) {
                        factory.literal(lit.value, lit.language)
                    } else if (lit.datatype.value.isNotEmpty()) {
                        factory.literal(lit.value, factory.namedNode(lit.datatype.value))
                    } else {
                        factory.literal(lit.value)
                    }
                }

                "DefaultGraph" -> factory.defaultGraph()
                "Quad" -> {
                    val nq = t as rdf.Quad
                    val s = toJsTerm(nq.subject)
                    val p = toJsTerm(nq.predicate)
                    val o = toJsTerm(nq.`object`)
                    val g = toJsTerm(nq.graph)
                    factory.quad(s, p, o, g)
                }

                else -> throw IllegalArgumentException("Unknown term type: ${t.termType}")
            }
        }

        val s = toJsTerm(q.subject)
        val p = toJsTerm(q.predicate)
        val o = toJsTerm(q.`object`)
        val g = toJsTerm(q.graph)

        quadsList.add(factory.quad(s, p, o, g))
    }
    return quadsList.toTypedArray()
}
