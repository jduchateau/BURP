package turtleprov

import rdfobjectloader.manifest.Manifest
import rdfobjectloader.manifest.TestCase

expect fun loadManifest(manifestPath: String): Manifest

expect fun isIsomorphic(graph1Path: String, graph2Path: String): Boolean

expect fun getManifestPath(basePath: String): String
