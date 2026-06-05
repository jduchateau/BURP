package burp

import burp.Main.doMain
import burp.util.getDecompressedFile
import burp.vocabularies.RER
import burp.vocabularies.RML
import com.opencsv.CSVReaderHeaderAware
import com.opencsv.CSVWriter
import com.opencsv.exceptions.CsvException
import kotlinx.serialization.json.Json
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RiotException
import org.apache.jena.sparql.core.DatasetGraph
import org.apache.jena.sparql.util.IsoMatcher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileReader
import java.io.IOException
import java.nio.file.*
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.io.path.absolutePathString

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestRMLModule {
    abstract fun getBase(): String

    @Throws(IOException::class, CsvException::class)
    open fun testDataProvider(): Stream<TestData> {
        val testCaseDir = Paths.get(getBase()).toAbsolutePath().normalize()

        val testDataList: MutableList<TestData?> = ArrayList<TestData?>()
        val csvFilePath = testCaseDir.resolve("./metadata.csv").normalize()
        val reader = CSVReaderHeaderAware(FileReader(csvFilePath.toFile()))

        var record: MutableMap<String, String>? = null
        while ((reader.readMap().also { record = it }) != null) {
            if (record == null) continue
            val td = TestData(record)
            testDataList.add(td)
        }

        return testDataList.sortedBy { td -> td?.ID }
            .filterNotNull()
            .stream()
    }


    fun getPath(testData: TestData, path: String): Path =
        Paths.get(getBase(), testData.ID, path).toAbsolutePath().normalize()

    fun getPathOptional(testData: TestData, path: String?): Path? =
        path?.let { Paths.get(getBase(), testData.ID, path).toAbsolutePath().normalize() }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    @Throws(Exception::class)
    open fun testDirectoryBasedCases(testData: TestData) {
        println("--------------------------------------------------------------------------------")
        System.out.printf("Processing test %s: %s%n", testData.ID, testData.title)
        println("--------------------------------------------------------------------------------")

        println("Mapping\t${getPath(testData, testData.mapping)}")
        println("First Input\t${getPathOptional(testData, testData.input1)}")
        println("First Output\t${getPathOptional(testData, testData.output1)}")
        println("Expects error?\t${testData.error}")
        println()

        if (testData.error) testForNotOK(testData)
        else testForOK(testData)
    }

    private fun getCompressionFromFileName(fileName: String): Resource? {
        if (fileName.endsWith(".tar.xz")) return RML.tarxz
        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) return RML.targz
        if (fileName.endsWith(".gz")) return RML.gzip
        if (fileName.endsWith(".zip")) return RML.zip
        return RML.none
    }

    fun testForOK(testData: TestData, mappingPath: String?) {
        val originalCwd = Path.of(getBase(), testData.ID).toAbsolutePath().normalize()
        val tempDir = Files.createTempDirectory(testData.ID)
        val outputs = arrayOf(testData.output1, testData.output2, testData.output3)
            .filterNotNull()
            .filter { it.isNotBlank() }

        Files.walk(originalCwd).use { stream ->
            stream.forEach { source ->
                val relative = originalCwd.relativize(source)
                val dest = tempDir.resolve(relative)
                if (!Files.isDirectory(dest) && relative.toString() !in outputs) {
                    Files.createDirectories(dest.parent)
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        val resultPath = tempDir.resolve(testData.output1?.takeIf { it.isNotEmpty() } ?: "default.nq").toString()
        val reportPath = Files.createTempFile("report_" + testData.ID, ".nq").toString()

        println("This test should generate a graph.")
        val tempMappingPath = tempDir.resolve("mapping.ttl").toString()

        val exit = doMain(
            arrayOf<String>(
                "-m",
                tempMappingPath,
                "-o",
                resultPath,
                "--baseIRI",
                testData.baseIRI,
                "--reportFile",
                reportPath
            ), tempDir
        )
        println("Exit code: $exit")

        for (out in outputs) {
            val expectedOutputPathStr = originalCwd.resolve(out).toString()
            val expectedOutputPath = Path.of(expectedOutputPathStr)
            val actualOutputPathStr = tempDir.resolve(out).toString()
            val actualOutputPath = Path.of(actualOutputPathStr)

            println("Checking output file: $out")
            println("Expected: $expectedOutputPath")
            println("Actual: $actualOutputPath")


            val expectedCompression = getCompressionFromFileName(out)
            var decompressedExpectedPath = expectedOutputPathStr
            if (expectedCompression !== RML.none && Files.exists(expectedOutputPath)) {
                println("Decompressing expected output: $expectedOutputPath")
                decompressedExpectedPath = getDecompressedFile(expectedOutputPathStr, expectedCompression, null)
            }

            val actualCompression = getCompressionFromFileName(out)
            var decompressedActualPath = actualOutputPathStr
            if (actualCompression !== RML.none && Files.exists(actualOutputPath)) {
                println("Decompressing actual output: $actualOutputPath")
                decompressedActualPath = getDecompressedFile(actualOutputPathStr, actualCompression, null)
            }

            val isNFormat = out.endsWith(".nt") || out.endsWith(".nq")
                    || out.matches(Regex(""".*\.(nt|nq)(\..*)?$"""))
            val isJsonFormat = out.endsWith(".json") || out.endsWith(".jsonld") || out.endsWith(".rdfjson")
            try {
                val expected = loadDataset(decompressedExpectedPath)
                val actual = loadDataset(decompressedActualPath)

                val isIsomorphic = IsoMatcher.isomorphic(expected, actual)
                if (!isIsomorphic) {
                    println("--- Expected")
                    RDFDataMgr.write(System.out, expected, Lang.TRIG)
                    println("--- Actual")
                    RDFDataMgr.write(System.out, actual, Lang.TRIG)
                }

                println("Isomorphic? " + (if (isIsomorphic) "OK" else "NOK"))
                Assertions.assertTrue(isIsomorphic, "is not isomorphic")
            } catch (e: Exception) {
                if (isNFormat) {
                    println("RDF parsing failed, falling back to line-by-line comparison: " + e.message)

                    val expectedData = Files.readString(Path.of(decompressedExpectedPath))
                    val actualData = Files.readString(Path.of(decompressedActualPath))

                    val expectedLines = normalizeAndDeduplicateLines(expectedData)
                    val actualLines = normalizeAndDeduplicateLines(actualData)

                    if (expectedLines == actualLines) {
                        println("Line comparison: OK - Matched by normalized line-by-line comparison")
                    } else {
                        println("--- Expected (normalized)")
                        expectedLines.forEach(Consumer { x: String? -> println(x) })
                        println("--- Actual (normalized)")
                        actualLines.forEach(Consumer { x: String? -> println(x) })
                        println("--- Actual (raw)")
                        println(actualData)
                        error("Expected and actual do not match in line-by-line comparison for $out")
                    }
                } else if (isJsonFormat) {
                    println("JSON parsing failed, falling back to deep JSON comparison: " + e.message)

                    val expectedData = Files.readString(Path.of(decompressedExpectedPath))
                    val actualData = Files.readString(Path.of(decompressedActualPath))

                    val expectedJson = Json.parseToJsonElement(expectedData)
                    val actualJson = Json.parseToJsonElement(actualData)

                    if (expectedJson == actualJson) {
                        println("JSON comparison: OK - Matched by deep JSON comparison")
                    } else {
                        println("--- Expected (JSON)")
                        println(expectedJson)
                        println("--- Actual (JSON)")
                        println(actualJson)
                        error("Expected and actual do not match in deep JSON comparison for $out")
                    }
                } else {
                    throw e
                }
            }
        }

        println("Exit code: $exit")

        //assertEquals(0, exit);
        val report = RDFDataMgr.loadModel(reportPath)
        val countErrors: Long = getCountErrors(report)
        val errorTypes: MutableList<String?> = getErrorTypes(report)
        if (countErrors > 0) {
            println("Error types: $errorTypes")
        }
    }

    @Throws(RiotException::class)
    private fun loadDataset(path: String): DatasetGraph {
        if (path.endsWith(".rdfjson")) {
            return RDFDataMgr.loadDatasetGraph(path, Lang.RDFJSON)
        }
        if (path.endsWith(".rdfxml")) {
            return RDFDataMgr.loadDatasetGraph(path, Lang.RDFXML)
        }
        return RDFDataMgr.loadDatasetGraph(path)
    }

    @Throws(IOException::class)
    fun testForOK(testData: TestData) {
        val m = getPath(testData, testData.mapping).absolutePathString()
        testForOK(testData, m)
    }

    @Throws(IOException::class)
    fun testForNotOK(testData: TestData, mappingPath: String?) {
        val resultPath = Files.createTempFile(null, ".nq").toString()
        val reportPath = Files.createTempFile("report_" + testData.ID, ".nq").toString()
        System.out.printf("Writing output to %s%n", resultPath)

        println("This test should NOT generate a graph.")
        val cwd = Path.of(getBase(), testData.ID).toAbsolutePath().normalize()
        val exit = doMain(
            arrayOf(
                "-m", mappingPath!!,
                "-o", resultPath,
                "--baseIRI", testData.baseIRI,
                "--reportFile", reportPath,
            ), cwd
        )

        val outputFileSize = Files.size(Paths.get(resultPath))
        println(if (outputFileSize == 0L) "No output file" else "Output file is not empty")

        if (outputFileSize != 0L) {
            val actual = RDFDataMgr.loadModel(resultPath)
            println("--- Actual")
            actual.write(System.out, "NQ")
        }

        val report = RDFDataMgr.loadModel(reportPath)
        println("--- Report")
        report.write(System.out, "Turtle")

        // Always write the test id to the error.csv file
        val errorCsv = Path.of(getBase(), "error.csv")
        if (!Files.exists(errorCsv)) Files.createFile(errorCsv)
        val writer = CSVWriter(Files.newBufferedWriter(errorCsv, StandardOpenOption.APPEND))
        writer.writeNext(arrayOf<String>(testData.ID))
        writer.flush()

        Assertions.assertTrue(exit > 0)
        Assertions.assertFalse(report.isEmpty())

        val countErrors: Long = getCountErrors(report)
        val errorTypes: MutableList<String?> = getErrorTypes(report)

        println("Error types: $errorTypes")
        Assertions.assertTrue(countErrors > 0, "Expected at least 1 error, but got $countErrors")

        println()

        // Append to error.csv in getBase()
        // header if not present: test case id, expected error
        // one line per test case
        val nextLine = ArrayList<String?>()
        nextLine.add(testData.ID)
        nextLine.add(testData.title)
        nextLine.add(countErrors.toString())
        nextLine.addAll(errorTypes)
        writer.writeNext(nextLine.toTypedArray<String?>())
        writer.flush()
    }

    @Throws(IOException::class)
    fun testForNotOK(testData: TestData) {
        val m = getPath(testData, testData.mapping).absolutePathString()
        testForNotOK(testData, m)
    }

    companion object {
        private fun getCountErrors(report: Model): Long {
            val countQueryString: String = """
                PREFIX rer: <${RER.NS}>
                SELECT (COUNT(?error) AS ?count) WHERE {
                  ?s rer:hasError ?error .
                }
                """.trimIndent()

            var countErrors: Long = 0
            QueryExecutionFactory.create(countQueryString, report).use { qexec ->
                val results = qexec.execSelect()
                if (results.hasNext()) {
                    val soln = results.nextSolution()
                    countErrors = soln.getLiteral("count").long
                }
            }
            return countErrors
        }

        private fun getErrorTypes(report: Model): MutableList<String?> {
            val typeQueryString: String = """
                PREFIX rer: <${RER.NS}>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?type WHERE {
                  ?s rer:hasError ?error .
                  ?error rdf:type ?type .
                }
                """.trimIndent()
            val errorTypes: MutableList<String?> = ArrayList<String?>()
            QueryExecutionFactory.create(typeQueryString, report).use { qexec ->
                val results = qexec.execSelect()
                while (results.hasNext()) {
                    val soln = results.nextSolution()
                    val typeInfo = soln.getResource("type")
                    if (typeInfo != null) {
                        errorTypes.add(typeInfo.localName)
                    }
                }
            }
            return errorTypes
        }

        /**
         * Normalizes and deduplicates lines for fallback line-by-line comparison.
         * Removes all whitespace from each line and sorts the result.
         */
        private fun normalizeAndDeduplicateLines(data: String): List<String> {
            val normalizedLines = mutableSetOf<String>()
            val lines = data.trim().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (line in lines) {
                val trimmed = line.trim()
                if (!trimmed.isEmpty()) {
                    // Normalize by removing all whitespace
                    val normalized = trimmed.replace("\\s+", " ")
                    normalizedLines.add(normalized)
                }
            }
            return normalizedLines.toList()
                .sortedWith { obj, anotherString -> obj!!.compareTo(anotherString!!) }
        }
    }
}
