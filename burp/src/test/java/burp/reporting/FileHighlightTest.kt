package burp.reporting

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import rdfobjectloader.Point
import rdfobjectloader.PointRange
import java.nio.file.Path
import kotlin.io.path.writeLines

class FileHighlightTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            // Try to force ANSI, but if it fails (stripped), we accept plain text in tests
            System.setProperty("picocli.ansi", "true")
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            System.clearProperty("picocli.ansi")
        }
    }

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test extractAndHighlight basic usage`() {
        val file = tempDir.resolve("test.ttl")
        val lines = listOf(
            "@prefix ex: <http://example.com/> .",
            "foo bar baz",
            "qux quux"
        )
        file.writeLines(lines)

        // Highlight "bar" on line 2 (indices 4..6)
        val nodes = listOf(
            PointRange(Point(1, 4), Point(1, 6))
        )

        val result = extractAndHighlight(file, nodes, contextLines = 0)

        println("Basic Result:\n$result")

        assertNotNull(result)
        // Line number formatting: "$lineNumber ".padStart(5) + "| "
        // "2 " -> "   2 | "
        assertTrue(result!!.contains("   2 | "), "Should contain formatted line number '   2 | '")
        assertTrue(result.contains("bar"), "Should contain highlighted text 'bar'")
    }

    @Test
    fun `test extractAndHighlight with context`() {
        val file = tempDir.resolve("context.ttl")
        val lines = listOf(
            "line 1",
            "line 2 target",
            "line 3"
        )
        file.writeLines(lines)

        // Highlight "target" on line 2 (cols 7..12)
        val nodes = listOf(
            PointRange(Point(1, 7), Point(1, 12))
        )

        val result = extractAndHighlight(file, nodes, contextLines = 1)

        // println("Context Result:\n$result")

        assertNotNull(result)
        // Check context lines and target line
        // "1 " -> "   1 | "
        assertTrue(result!!.contains("   1 | line 1"))
        assertTrue(result.contains("   2 | line 2 "))
        assertTrue(result.contains("target"))
        assertTrue(result.contains("   3 | line 3"))
    }

    @Test
    fun `test file not found`() {
        val file = tempDir.resolve("nonexistent.ttl")
        val nodes = listOf(PointRange(Point(1, 0), Point(1, 5)))

        val result = extractAndHighlight(file, nodes)
        assertNull(result)
    }

    @Test
    fun `test empty nodes`() {
        val file = tempDir.resolve("empty.ttl")
        file.writeLines(listOf("abc"))

        val result = extractAndHighlight(file, emptyList())
        assertNull(result)
    }

    @Test
    fun `test ellipsis insertion`() {
        val file = tempDir.resolve("ellipsis.ttl")
        val lines = (1..10).map { "line $it" }
        file.writeLines(lines)

        // Highlight line 2 and line 9. Context 0.
        // Should print line 2, then ellipsis, then line 9.

        val nodes = listOf(
            PointRange(Point(1, 0), Point(1, 5)),
            PointRange(Point(8, 0), Point(8, 5))
        )

        val result = extractAndHighlight(file, nodes, contextLines = 0)

        // println("Ellipsis Result:\n$result")

        assertNotNull(result)

        assertTrue(result!!.contains("   2 | "), "Should contain line 2")
        assertTrue(result.contains("   ..."), "Should contain ellipsis")
        assertTrue(result.contains("   9 | "), "Should contain line 9")
    }
}
