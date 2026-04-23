package burp

import java.util.stream.Stream

class TestSparqlCDT : TestRMLModule() {
    override fun getBase() = "./src/test/resources/extra/";

    override fun testDataProvider(): Stream<TestData?>? {
        return Stream.of(
            TestData(
                "cdt01",
                "Test CDT for List and Maps for RDF Literals",
                "description",
                "Extra-CDT",
                "http://example.org/",
                "mapping.ttl",
                "application/json",
                null,
                null,
                "application/n-quads",
                null,
                null,
                "example.json",
                null,
                null,
                "output.nq",
                null,
                null,
                false
            ),
        )
    }

}