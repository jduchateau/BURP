package burp;

import burp.vocabularies.RER;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.IsoMatcher;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestRMLModule {

    public abstract String getBase();

    Stream<TestData> testDataProvider() throws IOException, CsvException {
        Path testCaseDir = Paths.get(getBase()).toAbsolutePath().normalize();

        List<TestData> testDataList = new ArrayList<TestData>();
        Path csvFilePath = testCaseDir.resolve("./metadata.csv").normalize();
        var reader = new CSVReaderHeaderAware(new FileReader(csvFilePath.toFile()));

        Map<String, String> record;
        while ((record = reader.readMap()) != null) {
            TestData td = new TestData(record);
            testDataList.add(td);
        }

        return testDataList.stream().sorted(Comparator.comparing(t -> t.ID));
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    void testDirectoryBasedCases(TestData testData) throws Exception {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Processing test %s: %s%n", testData.ID, testData.title);
        System.out.println("--------------------------------------------------------------------------------");

        System.out.println(testData.mapping);
        System.out.println(testData.output1);
        System.out.println(testData.error);
        System.out.println();

        if (testData.error) testForNotOK(testData);
        else testForOK(testData);
    }

    public void testForOK(TestData testData, String mappingPath) throws IOException {
        String resultPath = Files.createTempFile(null, ".nq").toString();
        System.out.printf("Writing output to %s%n", resultPath);

        String reportPath = Files.createTempFile("report_" + testData.ID, ".nq").toString();

        System.out.println("This test should generate a graph.");
        String expectedOutputPath = Path.of(getBase(), testData.ID, testData.output1).toAbsolutePath().normalize().toString();

        Path cwd = Path.of(getBase(), testData.ID).toAbsolutePath().normalize();
        int exit = Main.INSTANCE.doMain(new String[]{"-m", mappingPath, "-o", resultPath, "--baseIRI", testData.baseIRI, "--reportFile", reportPath,}, cwd);

        DatasetGraph expected = RDFDataMgr.loadDatasetGraph(expectedOutputPath);
        DatasetGraph actual = RDFDataMgr.loadDatasetGraph(resultPath);

        boolean isIsomorphic = IsoMatcher.isomorphic(expected, actual);
        if (!isIsomorphic) {
            System.out.println("--- Expected");
            RDFDataMgr.write(System.out, expected, Lang.TRIG);
            System.out.println("--- Actual");
            RDFDataMgr.write(System.out, actual, Lang.TRIG);
        }

        System.out.println("Isomorphic? " + (isIsomorphic ? "OK" : "NOK"));
        assertTrue(isIsomorphic);

        System.out.println("Exit code: " + exit);
        //assertEquals(0, exit);

        Model report = RDFDataMgr.loadModel(reportPath);
        long countErrors = getCountErrors(report);
        List<String> errorTypes = getErrorTypes(report);
        if (countErrors > 0) {
            System.out.println("Error types: " + errorTypes);
        }
    }

    public void testForOK(TestData testData) throws IOException {
        String m = Path.of(getBase(), testData.ID, testData.mapping).toAbsolutePath().normalize().toString();
        testForOK(testData, m);
    }

    public void testForNotOK(TestData testData, String mappingPath) throws IOException {
        String resultPath = Files.createTempFile(null, ".nq").toString();
        String reportPath = Files.createTempFile("report_" + testData.ID, ".nq").toString();
        System.out.printf("Writing output to %s%n", resultPath);

        System.out.println("This test should NOT generate a graph.");
        Path cwd = Path.of(getBase(), testData.ID).toAbsolutePath().normalize();
        int exit = Main.INSTANCE.doMain(new String[]{"-m", mappingPath, "-o", resultPath, "--baseIRI", testData.baseIRI, "--reportFile", reportPath,}, cwd);

        long outputFileSize = Files.size(Paths.get(resultPath));
        System.out.println(outputFileSize == 0 ? "No output file" : "Output file is not empty");

        if (outputFileSize != 0) {
            Model actual = RDFDataMgr.loadModel(resultPath);
            System.out.println("--- Actual");
            actual.write(System.out, "NQ");
        }

        Model report = RDFDataMgr.loadModel(reportPath);
        System.out.println("--- Report");
        report.write(System.out, "Turtle");

        // Always write the test id to the error.csv file
        Path errorCsv = Path.of(getBase(), "error.csv");
        if (!Files.exists(errorCsv)) Files.createFile(errorCsv);
        CSVWriter writer = new CSVWriter(Files.newBufferedWriter(errorCsv, StandardOpenOption.APPEND));
        writer.writeNext(new String[]{testData.ID});
        writer.flush();

        assertTrue(exit > 0);
        assertFalse(report.isEmpty());

        long countErrors = getCountErrors(report);
        List<String> errorTypes = getErrorTypes(report);

        System.out.println("Error types: " + errorTypes);
        assertTrue(countErrors > 0, "Expected at least 1 error, but got " + countErrors);

        System.out.println();

        // Append to error.csv in getBase()
        // header if not present: test case id, expected error
        // one line per test case
        var nextLine = new ArrayList<String>();
        nextLine.add(testData.ID);
        nextLine.add(testData.title);
        nextLine.add(String.valueOf(countErrors));
        nextLine.addAll(errorTypes);
        writer.writeNext(nextLine.toArray(new String[0]));
        writer.flush();
    }

    private static long getCountErrors(@NonNull Model report) {
        String countQueryString = """
                PREFIX rer: <%s>
                SELECT (COUNT(?error) AS ?count) WHERE {
                  ?s rer:hasError ?error .
                }""".formatted(RER.NS);

        long countErrors = 0;
        try (var qexec = QueryExecutionFactory.create(countQueryString, report)) {
            var results = qexec.execSelect();
            if (results.hasNext()) {
                var soln = results.nextSolution();
                countErrors = soln.getLiteral("count").getLong();
            }
        }
        return countErrors;
    }

    private static @NonNull List<String> getErrorTypes(@NonNull Model report) {
        String typeQueryString = """
                PREFIX rer: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?type WHERE {
                  ?s rer:hasError ?error .
                  ?error rdf:type ?type .
                }""".formatted(RER.NS);
        List<String> errorTypes = new ArrayList<>();
        try (var qexec = QueryExecutionFactory.create(typeQueryString, report)) {
            var results = qexec.execSelect();
            while (results.hasNext()) {
                var soln = results.nextSolution();
                var typeInfo = soln.getResource("type");
                if (typeInfo != null) {
                    errorTypes.add(typeInfo.getLocalName());
                }
            }
        }
        return errorTypes;
    }

    public void testForNotOK(TestData testData) throws IOException {
        String m = new File(getBase() + testData.ID, testData.mapping).getAbsolutePath();
        testForNotOK(testData, m);
    }
}
