package burp

import burp.vocabularies.D2RQ
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.update.UpdateAction
import org.apache.jena.update.UpdateFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mssqlserver.MSSQLServerContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream

class TestRMLIORegistry : TestRMLModule() {
    override fun getBase(): String {
        return base
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    @Throws(Exception::class)
    override fun testDirectoryBasedCases(testData: TestData) {
        println("--------------------------------------------------------------------------------")
        System.out.printf("Processing test %s%n", testData.ID)
        println("--------------------------------------------------------------------------------")

        val mappingPath: String = File(base + testData.ID, testData.mapping).getAbsolutePath()
        var newMappingPath = mappingPath

        if (testData.input_format1 == "application/sql") {
            println("Preparing mapping for database connection...")
            val model = RDFDataMgr.loadModel(mappingPath)
            val jdbcDriver = model.listObjectsOfProperty(D2RQ.jdbcDriver).next().asLiteral().getString()

            var queryString = ""
            val db: JdbcDatabaseContainer<*> = when (jdbcDriver) {
                "com.mysql.cj.jdbc.Driver" -> {
                    MYSQL_CONTAINER_FUTURE!!.join()
                    queryString = "?allowMultiQueries=true"
                    MYSQL_CONTAINER.addParameter("allowMultiQueries", "true")
                    MYSQL_CONTAINER
                }

                "org.postgresql.Driver" -> {
                    PGSQL_CONTAINER_FUTURE!!.join()
                    PGSQL_CONTAINER
                }

                "com.microsoft.sqlserver.jdbc.SQLServerDriver" -> {
                    MSSQL_CONTAINER_FUTURE!!.join()
                    MSSQL_CONTAINER.withUrlParam("databaseName", "master")
                    MSSQL_CONTAINER.createConnection("").use { conn ->
                        conn.createStatement().use { stmt ->
                            stmt.executeUpdate("IF EXISTS (SELECT name FROM sys.databases WHERE name = N'TestDB') ALTER DATABASE [TestDB] SET  SINGLE_USER WITH ROLLBACK IMMEDIATE")
                            stmt.executeUpdate("IF EXISTS (SELECT name FROM sys.databases WHERE name = N'TestDB') DROP DATABASE [TestDB]")
                            stmt.executeUpdate("CREATE DATABASE [TestDB]")
                        }
                    }
                    MSSQL_CONTAINER.withUrlParam("databaseName", "TestDB")
                    MSSQL_CONTAINER
                }

                else -> throw IllegalArgumentException("Unsupported JDBC driver: " + jdbcDriver)
            }

            val sparql = String.format(
                """
                            PREFIX d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#>
                            DELETE { ?s d2rq:jdbcDSN ?oldDSN; d2rq:username ?oldUser; d2rq:password ?oldPass }
                            INSERT { ?s d2rq:jdbcDSN "%s"; d2rq:username "%s"; d2rq:password "%s" }
                            WHERE { ?s d2rq:jdbcDSN ?oldDSN; d2rq:username ?oldUser; d2rq:password ?oldPass }
                            """.trimIndent(),
                db.getJdbcUrl(), db.getUsername(), db.getPassword()
            )
            val updateRequest = UpdateFactory.create(sparql)
            UpdateAction.execute(updateRequest, model)

            newMappingPath = Paths.get(base + testData.ID, "mapping-new.ttl").toAbsolutePath().toString()
            FileOutputStream(newMappingPath).use { out ->
                RDFDataMgr.write(out, model, RDFFormat.TURTLE_PRETTY)
            }
            println("Populating database...")
            val sqlFilePath: String = File(base + testData.ID, testData.input1).getAbsolutePath()
            val sql = String(Files.readAllBytes(Paths.get(sqlFilePath)))
            db.createConnection(queryString).use { conn ->
                conn.createStatement().use { stmt ->
                    val update = stmt.executeUpdate(sql)
                    if (update > 0) {
                        println("SQL update successfully.")
                    }
                }
            }
        }

        println(testData.mapping)
        println(testData.output1)
        println(testData.error)
        println()

        if (testData.error) testForNotOK(testData, newMappingPath)
        else testForOK(testData, newMappingPath)
    }

    companion object {
        var base: String = "./src/test/resources/rml-io-registry/"

        var PGSQL_CONTAINER: PostgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withUsername("postgres")
            .withPassword("test")
        private var PGSQL_CONTAINER_FUTURE: CompletableFuture<Void?>? = null

        var MYSQL_CONTAINER: MySQLContainer = MySQLContainer("mysql:8")
            .withEnv("MYSQL_ROOT_HOST", "%")
            .withCommand("mysqld", "--sql_mode=ANSI_QUOTES")
        private var MYSQL_CONTAINER_FUTURE: CompletableFuture<Void?>? = null

        var MSSQL_CONTAINER: MSSQLServerContainer =
            MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04")
                .acceptLicense()
        private var MSSQL_CONTAINER_FUTURE: CompletableFuture<Void?>? = null

        @JvmStatic
        @BeforeAll
        fun startContainers(): Unit {
            PGSQL_CONTAINER_FUTURE = CompletableFuture.runAsync(Runnable { PGSQL_CONTAINER.start() })
            MYSQL_CONTAINER_FUTURE = CompletableFuture.runAsync(Runnable { MYSQL_CONTAINER.start() })
            MSSQL_CONTAINER_FUTURE = CompletableFuture.runAsync(Runnable { MSSQL_CONTAINER.start() })
        }

        @JvmStatic
        @AfterAll
        fun stopContainers(): Unit {
            Stream.of<JdbcDatabaseContainer<out JdbcDatabaseContainer<*>?>?>(
                PGSQL_CONTAINER,
                MYSQL_CONTAINER,
                MSSQL_CONTAINER
            ).parallel().forEach { obj: JdbcDatabaseContainer<out JdbcDatabaseContainer<*>?>? -> obj!!.stop() }
        }
    }
}
