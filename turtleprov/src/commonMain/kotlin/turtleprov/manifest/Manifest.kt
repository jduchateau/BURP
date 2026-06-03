package turtleprov.manifest

import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfType
import turtleprov.manifest.gen.RdfManifest
import turtleprov.manifest.gen.RdfTest

@RdfType(RdfManifest.Manifest)
class Manifest {
    @RdfProperty("http://www.w3.org/2000/01/rdf-schema#label")
    var label: String? = null

    @RdfProperty(RdfManifest.entries)
    var entries: List<TestCase> = emptyList()

    @RdfProperty(RdfManifest.include)
    var include: List<String> = emptyList()

    var includedManifest: Map<String, Manifest> = mutableMapOf()
}

abstract class TestCase {
    @RdfProperty(RdfManifest.name)
    lateinit var name: String

    @RdfProperty(RdfManifest.action)
    lateinit var action: String
}

@RdfType(RdfTest.TestTurtleEval)
open class TestTurtleEval : TestCase() {
    @RdfProperty(RdfManifest.result)
    lateinit var result: String
}

@RdfType(RdfTest.TestTurtleNegativeEval)
class TestTurtleNegativeEval : TestCase()

@RdfType(RdfTest.TestTurtlePositiveSyntax)
class TestTurtlePositiveSyntax : TestCase()

@RdfType(RdfTest.TestTurtleNegativeSyntax)
class TestTurtleNegativeSyntax : TestCase()
