package turtleprov

import rdfobjectloader.manifest.Manifest

actual fun loadManifest(manifestPath: String): Manifest {
    throw UnsupportedOperationException("rdf-object-loader not implemented for JS yet")
}

actual fun isIsomorphic(graph1Path: String, graph2Path: String): Boolean {
    // We will prepare the js test using n3 and rdf-isomorphic
    // But since loadManifest throws, this won't be reached until loader is implemented.
    throw UnsupportedOperationException("Isomorphism JS not fully wired because loader isn't available")
}

actual fun getManifestPath(basePath: String): String {
    return "$basePath/manifest.ttl"
}
