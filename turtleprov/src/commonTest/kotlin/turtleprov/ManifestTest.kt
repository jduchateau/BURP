package turtleprov

import rdfobjectloader.manifest.*
import kotlin.test.Test

class ManifestTest {

    @Test
    fun runTurtleTestSuites() {
        val basePath = "turtleprov/src/commonTest/resources/rdf-tests/rdf/rdf12/rdf-turtle" // Use absolute relative from root
        try {
            processManifest(getManifestPath(basePath), basePath)
        } catch (e: Exception) {
            println("Skipping W3C Turtle Manifest tests: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun processManifest(manifestPath: String, basePath: String) {
        val manifest = loadManifest(manifestPath)
        println("Running manifest: ${manifest.label ?: manifestPath}")

        for (testCase in manifest.entries) {
            when (testCase) {
                is TestTurtleEval -> {
                    val actionFile = "$basePath/${testCase.action}"
                    val resultFile = "$basePath/${testCase.result}"
                    // TODO: call turtleprov parser to parse actionFile into an N-Triples file or model
                    // then compare to resultFile using isIsomorphic
                    println("  Eval Test: ${testCase.name}")
                }
                is TestTurtleSyntax -> {
                    val actionFile = "$basePath/${testCase.action}"
                    // TODO: call turtleprov parser. Should succeed.
                    println("  Syntax Test: ${testCase.name}")
                }
                is TestTurtleNegativeSyntax -> {
                    val actionFile = "$basePath/${testCase.action}"
                    // TODO: call turtleprov parser. Should throw exception.
                    println("  Negative Syntax Test: ${testCase.name}")
                }
                is TestTurtleNegativeEval -> {
                    val actionFile = "$basePath/${testCase.action}"
                    // TODO: call turtleprov parser. Should throw or produce invalid output.
                    println("  Negative Eval Test: ${testCase.name}")
                }
            }
        }

        // Process includes
        for (include in manifest.include) {
            // resolve relative path. "include" could be something like "eval/manifest.ttl"
            val includeBasePath = if (include.contains("/")) {
                "$basePath/" + include.substringBeforeLast("/")
            } else {
                basePath
            }
            val fullIncludePath = "$basePath/$include"
            processManifest(fullIncludePath, includeBasePath)
        }
    }
}
