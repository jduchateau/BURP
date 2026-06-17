package turtleprov

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.riot.RDFDataMgr
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import turtleprov.manifest.gen.RdfManifest
import turtleprov.manifest.gen.RdfTest
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractManifestTest {

    abstract fun rootManifestFile(): Path
    abstract fun filterRdf11(): Boolean

    data class TestInfo(
        val testUri: String,
        val name: String,
        val actionFile: Path,
        val resultFile: Path?
    ) {
        override fun toString(): String {
            return "$name (${actionFile.fileName})"
        }
    }

    // Recursively find all manifests starting from a root manifest file
    private fun loadAllManifests(manifestFile: Path, visited: MutableSet<Path> = mutableSetOf()): List<Path> {
        val canonical = manifestFile.toAbsolutePath().normalize()
        if (!visited.add(canonical)) return emptyList()

        // If filterRdf11 is true, skip loading manifest files under rdf11/
        if (filterRdf11() && canonical.toString().contains("rdf11/")) {
            return emptyList()
        }

        val list = mutableListOf(canonical)
        val model = try {
            RDFDataMgr.loadModel(canonical.toString())
        } catch (e: Exception) {
            return emptyList()
        }

        val includeProp = model.createProperty(RdfManifest.include)
        val manifestType = model.createResource(RdfManifest.Manifest)
        val manifests = model.listResourcesWithProperty(org.apache.jena.vocabulary.RDF.type, manifestType)

        for (m in manifests) {
            val includeNode = m.getProperty(includeProp)?.`object`
            if (includeNode != null && includeNode.canAs(org.apache.jena.rdf.model.RDFList::class.java)) {
                val rdfList = includeNode.asResource().`as`(org.apache.jena.rdf.model.RDFList::class.java)
                for (node in rdfList.asJavaList()) {
                    if (node.isResource) {
                        val uri = node.asResource().uri
                        if (uri != null && uri.startsWith("file:")) {
                            val file = Path.of(URI(uri))
                            list.addAll(loadAllManifests(file, visited))
                        }
                    }
                }
            }
        }
        return list
    }

    private fun getTestsOfType(typeUri: String): List<TestInfo> {
        val manifests = loadAllManifests(rootManifestFile())
        val list = mutableListOf<TestInfo>()
        
        val queryStr = """
            PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX mf:   <${RdfManifest.NS}>
            PREFIX rdft: <${RdfTest.NS}>

            SELECT ?test ?name ?action ?result WHERE {
              ?manifest rdf:type mf:Manifest .
              ?manifest mf:entries ?entries .
              ?entries rdf:rest*/rdf:first ?test .
              ?test rdf:type <$typeUri> .
              ?test mf:name ?name .
              ?test mf:action ?action .
              OPTIONAL { ?test mf:result ?result }
            }
        """.trimIndent()

        val query = QueryFactory.create(queryStr)

        for (manifestFile in manifests) {
            val model = RDFDataMgr.loadModel(manifestFile.toString())
            QueryExecutionFactory.create(query, model).use { qexec ->
                val results = qexec.execSelect()
                while (results.hasNext()) {
                    val soln = results.nextSolution()
                    val testUri = soln.getResource("test").uri
                    val name = soln.getLiteral("name").string
                    val actionNode = soln.getResource("action")
                    val actionFile = Path.of(URI(actionNode.uri))
                    val resultNode = soln.getResource("result")
                    val resultFile = resultNode?.let { Path.of(URI(it.uri)) }
                    list.add(TestInfo(testUri, name, actionFile, resultFile))
                }
            }
        }
        return list
    }

    fun positiveSyntaxTests(): Stream<Arguments> {
        return getTestsOfType(RdfTest.TestTurtlePositiveSyntax)
            .map { Arguments.of(it) }.stream()
    }

    fun negativeSyntaxTests(): Stream<Arguments> {
        return getTestsOfType(RdfTest.TestTurtleNegativeSyntax)
            .map { Arguments.of(it) }.stream()
    }

    fun evalTests(): Stream<Arguments> {
        return getTestsOfType(RdfTest.TestTurtleEval)
            .map { Arguments.of(it) }.stream()
    }

    fun negativeEvalTests(): Stream<Arguments> {
        return getTestsOfType(RdfTest.TestTurtleNegativeEval)
            .map { Arguments.of(it) }.stream()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("positiveSyntaxTests")
    fun testPositiveSyntax(info: TestInfo) {
        parseTurtleFromString(Files.readString(info.actionFile))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeSyntaxTests")
    fun testNegativeSyntax(info: TestInfo) {
        assertThrows<Exception> {
            parseTurtleFromString(Files.readString(info.actionFile))
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("evalTests")
    fun testEvaluation(info: TestInfo) {
        val store = parseTurtleFromString(Files.readString(info.actionFile))
        val actionModel = store.toModel(withAnnotations = false)
        val expectedModel = RDFDataMgr.loadModel(info.resultFile!!.toString())
        assertTrue(expectedModel.isIsomorphicWith(actionModel)) {
            "Expected isomorphic models for test ${info.name}.\nExpected:\n${expectedModel}\nActual:\n${actionModel}"
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeEvalTests")
    fun testNegativeEvaluation(info: TestInfo) {
        try {
            val store = parseTurtleFromString(Files.readString(info.actionFile))
            assertTrue(store.triples.isEmpty())
        } catch (e: Exception) {
            // Expected parsing/evaluation failure
        }
    }
}
