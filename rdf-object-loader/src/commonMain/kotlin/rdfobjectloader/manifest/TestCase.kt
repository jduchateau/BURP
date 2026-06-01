package rdfobjectloader.manifest

import rdfobjectloader.annotations.*

abstract class TestCase {
    @RdfProperty("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#name")
    var name: String? = null

    @RdfProperty("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#action")
    var action: String? = null

    @RdfProperty("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#result")
    var result: String? = null
}

@RdfType("http://www.w3.org/ns/rdftest#TestTurtleEval")
class TestTurtleEval : TestCase()

@RdfType("http://www.w3.org/ns/rdftest#TestTurtleSyntax")
class TestTurtleSyntax : TestCase()

@RdfType("http://www.w3.org/ns/rdftest#TestTurtleNegativeSyntax")
class TestTurtleNegativeSyntax : TestCase()

@RdfType("http://www.w3.org/ns/rdftest#TestTurtleNegativeEval")
class TestTurtleNegativeEval : TestCase()

// RML Test Case
@RdfType("http://www.w3.org/2006/03/test-description#TestCase")
class RmlTestCase : TestCase() {
    @RdfId
    lateinit var id: String

    @RdfProperty("http://purl.org/dc/terms/identifier")
    var identifier: String? = null

    @RdfProperty("http://w3id.org/rml/test/hasError")
    var hasError: Boolean = false

    @RdfProperty("http://w3id.org/rml/test/mappingDocument")
    var mappingDocument: String? = null

    @RdfProperty("http://w3id.org/rml/test/defaultBaseIRI")
    var defaultBaseIRI: String? = null

    @RdfProperty("http://w3id.org/rml/test/input")
    var inputs: List<RmlTestInput> = emptyList()

    @RdfProperty("http://w3id.org/rml/test/output")
    var outputs: List<RmlTestOutput> = emptyList()
}

@RdfType("http://w3id.org/rml/test/Input")
class RmlTestInput {
    @RdfProperty("http://w3id.org/rml/test/input")
    var input: String? = null

    @RdfProperty("http://w3id.org/rml/test/inputFormat")
    var inputFormat: String? = null
}

@RdfType("http://w3id.org/rml/test/Output")
class RmlTestOutput {
    @RdfProperty("http://w3id.org/rml/test/output")
    var output: String? = null

    @RdfProperty("http://w3id.org/rml/test/outputFormat")
    var outputFormat: String? = null
}
