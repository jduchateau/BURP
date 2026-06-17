package turtleprov

import java.nio.file.Path

class Rdf11ManifestTest : AbstractManifestTest() {
    override fun rootManifestFile() = Path.of("src/test/resources/rdf-tests/rdf/rdf11/rdf-turtle/manifest.ttl")
    override fun filterRdf11() = false
}
