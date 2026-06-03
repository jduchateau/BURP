package turtleprov

import rdf.DatasetCore
import rdfkt.InMemoryDatasetCore
import turtleprov.manifest.*
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

expect fun loadManifest(manifestPath: String): Manifest

// Only uses the Default Graph.
expect fun loadRdfGraphInNt(path: String): DatasetCore
expect fun isIsomorphic(expectedGraph: DatasetCore, actualGraph: DatasetCore): Boolean

fun cleanPath(pathUri: String): String = if (pathUri.startsWith("file://")) {
    pathUri.removePrefix("file://")
} else if (!pathUri.startsWith("/")) {
    "/home/jakub/Documents/Dev/BURP/$pathUri"
} else {
    pathUri
}

class ManifestTest {
    @Test
    fun manifest() {
        val basePath = "turtleprov/src/commonTest/resources/rdf-tests/rdf/rdf12/rdf-turtle"
        val manifestPath = "$basePath/manifest.ttl"

        val manifest = loadManifest(cleanPath(manifestPath))
        val allTestCases = manifest.includedManifest.values.flatMap { it.entries }

        println("Manifest: ${manifest.label} contains ${allTestCases.size} test cases")

        var failedTestCount = 0
        for ((idx, testCase) in allTestCases.withIndex()) {
            try {
                when (testCase) {
                    is TestTurtleEval -> {
                        println("Eval: ${testCase.name}")
                        val action = parseTurtleFromFile(cleanPath(testCase.action))
                        val actionQuads = RDF12Converter(false).toQuads(action)
                        val actionGraph = InMemoryDatasetCore(actionQuads.toMutableSet())
                        val result = loadRdfGraphInNt(cleanPath(testCase.result))
                        assertTrue(isIsomorphic(result, actionGraph), "Expected isomorphism")
                    }

                    is TestTurtleNegativeEval -> {
                        println("Negative Eval: ${testCase.name}")
                        // TODO: Description says we should compare result for non-isomorphism
                        //  but we don't get the file to test against and even if we had how to load it?
                        val action = parseTurtleFromFile(cleanPath(testCase.action))
                        assertTrue(action.quads.isEmpty(), "Expected failed eval")
                    }

                    is TestTurtlePositiveSyntax -> {
                        println("Syntax: ${testCase.name}")
                        parseTurtleFromFile(cleanPath(testCase.action))
                    }

                    is TestTurtleNegativeSyntax -> {
                        println("Negative Syntax: ${testCase.name}")
                        assertFailsWith<Exception>("Expected failed syntax") {
                            parseTurtleFromFile(cleanPath(testCase.action))
                        }
                    }
                }
            } catch (e: Throwable) {
                println("Error in test case $idx ${testCase.name}: $e")
                failedTestCount++
            }
        }
        val passedTestCount = allTestCases.size - failedTestCount
        val passedPercentage = (passedTestCount.toDouble() / allTestCases.size) * 100
        val failedPercentage = (failedTestCount.toDouble() / allTestCases.size) * 100
        println(
            """Manifest ${manifest.label}: 
            |  passed tests $passedTestCount ($passedPercentage%)
            |  failed tests $failedTestCount ($failedPercentage%)
            |   total tests ${allTestCases.size}""".trimMargin()
        )
    }
}
