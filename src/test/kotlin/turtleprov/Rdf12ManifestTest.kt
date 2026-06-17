package turtleprov

import java.nio.file.Path

class Rdf12ManifestTest : AbstractManifestTest() {
    override fun rootManifestFile() = Path.of("src/test/resources/rdf-tests/rdf/rdf12/rdf-turtle/manifest.ttl")
    override fun filterRdf11() = true
}
