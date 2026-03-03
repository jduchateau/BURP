package burp;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TestRMLModule {

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
        int exit = Main.INSTANCE.doMain(new String[]{"-m", mappingPath, "-o", resultPath, "--baseIRI", testData.baseIRI, "--reportFile", reportPath}, cwd);

        Model expected = RDFDataMgr.loadModel(expectedOutputPath);
        Model actual = RDFDataMgr.loadModel(resultPath);

        boolean isIsomorphic = expected.isIsomorphicWith(actual);
        if (!isIsomorphic) {
            System.out.println("--- Expected");
            expected.write(System.out, "Turtle");
            System.out.println("--- Actual");
            actual.write(System.out, "Turtle");
        }

        System.out.println(isIsomorphic ? "OK" : "NOK");
        assertTrue(isIsomorphic);

        assertEquals(0, exit);
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
        int exit = Main.INSTANCE.doMain(new String[]{"-m", mappingPath, "-o", resultPath, "--baseIRI", testData.baseIRI, "--reportFile", reportPath}, cwd);

        long outputFileSize = Files.size(Paths.get(resultPath));
        System.out.println(outputFileSize == 0 ? "OK" : "NOK");

        if (outputFileSize != 0) {
            Model actual = RDFDataMgr.loadModel(resultPath);
            System.out.println("--- Actual");
            actual.write(System.out, "NQ");
        }

        Model report = RDFDataMgr.loadModel(reportPath);
        System.out.println("--- Report");
        report.write(System.out, "Turtle");

        assertTrue(exit > 0);
        assertEquals(0, outputFileSize);
        assertFalse(report.isEmpty());

        System.out.println();
    }

    public void testForNotOK(TestData testData) throws IOException {
        String m = new File(getBase() + testData.ID, testData.mapping).getAbsolutePath();
        testForNotOK(testData, m);
    }

}
