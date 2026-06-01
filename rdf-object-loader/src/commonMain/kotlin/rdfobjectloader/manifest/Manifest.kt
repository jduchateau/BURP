package rdfobjectloader.manifest

import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfType

@RdfType("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#Manifest")
class Manifest {
    @RdfProperty("http://www.w3.org/2000/01/rdf-schema#label")
    var label: String? = null

    @RdfProperty("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#entries")
    var entries: List<TestCase> = emptyList()

    @RdfProperty("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#include")
    var include: List<String> = emptyList() // URIs to included manifests
}
