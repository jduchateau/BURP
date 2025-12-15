package burp.reporting

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import turtleprov.NodeInfo
import turtleprov.Point

class FileGeometryTest {

    @Test
    fun `test user reproduction case with merged ranges`() {
        val lines = listOf(
            "@prefix ex: <http://example.com/> .",
            "@prefix rml: <http://w3id.org/rml/> .",
            "\"\"",
            "<http://example.com/base/TriplesMap1> a rml:TriplesMap;",
            "  rml:logicalSource [ a rml:LogicalSource;",
            "\"      rml:iterator \"\"$.students[*]\"\";\"",
            "      rml:referenceFormulation rml:JSONPath;",
            "      rml:source [ a rml:RelativePathSource;",
            "          rml:root rml:MappingDirectory;",
            "\"          rml:path \"\"student2.json\"\"\""
        )

        // User nodes:
        // "Point(line=10, column=10)","Point(line=10, column=18)"
        // "Point(line=10, column=20)","Point(line=10, column=33)"
        // "Point(line=9, column=10)","Point(line=9, column=18)"
        // "Point(line=9, column=19)","Point(line=9, column=39)"

        // Note: Point(line=10) corresponds to index 9.
        // NodeInfo subtracts 1 from line.

        val nodes = listOf(
            // Line 10 (index 9): 10..18
            NodeInfo(null, Point(10, 10), Point(10, 18), null, null, null),
            // Line 10 (index 9): 20..33
            NodeInfo(null, Point(10, 20), Point(10, 33), null, null, null),
            // Line 9 (index 8): 10..18
            NodeInfo(null, Point(9, 10), Point(9, 18), null, null, null),
            // Line 9 (index 8): 19..39 (Adjacent to 18)
            NodeInfo(null, Point(9, 19), Point(9, 39), null, null, null)
        )

        val result = getMergedHighlights(nodes, lines)

        // Expectation:
        // Index 9: [10..18, 20..33] (Separate)
        // Index 8: [10..39] (Merged 10..18 and 19..39)

        assertTrue(result.containsKey(9), "Result should contain index 9")
        assertTrue(result.containsKey(8), "Result should contain index 8")

        val ranges9 = result[9]!!
        assertEquals(2, ranges9.size)
        assertEquals(10..18, ranges9[0])
        assertEquals(20..33, ranges9[1])

        val ranges8 = result[8]!!
        assertEquals(1, ranges8.size)
        assertEquals(10..39, ranges8[0])
    }

    @Test
    fun `test overlapping ranges merge`() {
        val lines = listOf("0123456789")
        // Overlapping: 0..4 and 2..6 -> 0..6
        val nodes = listOf(
            NodeInfo(null, Point(1, 0), Point(1, 4), null, null, null),
            NodeInfo(null, Point(1, 2), Point(1, 6), null, null, null)
        )

        val result = getMergedHighlights(nodes, lines)
        val ranges = result[0]!!

        assertEquals(1, ranges.size)
        assertEquals(0..6, ranges[0])
    }

    @Test
    fun `test adjacent ranges merge`() {
        val lines = listOf("0123456789")
        // Adjacent: 0..4 and 5..9 -> 0..9
        val nodes = listOf(
            NodeInfo(null, Point(1, 0), Point(1, 4), null, null, null),
            NodeInfo(null, Point(1, 5), Point(1, 9), null, null, null)
        )

        val result = getMergedHighlights(nodes, lines)
        val ranges = result[0]!!

        assertEquals(1, ranges.size)
        assertEquals(0..9, ranges[0])
    }

    @Test
    fun `test non-adjacent ranges do not merge`() {
        val lines = listOf("0123456789")
        // Gap: 0..3 and 5..8 (Gap at 4) -> 0..3, 5..8
        val nodes = listOf(
            NodeInfo(null, Point(1, 0), Point(1, 3), null, null, null),
            NodeInfo(null, Point(1, 5), Point(1, 8), null, null, null)
        )

        val result = getMergedHighlights(nodes, lines)
        val ranges = result[0]!!

        assertEquals(2, ranges.size)
        assertEquals(0..3, ranges[0])
        assertEquals(5..8, ranges[1])
    }

    @Test
    fun `test multi-line node`() {
        val lines = listOf("line1", "line2", "line3")
        // Node spans from line 1 col 2 to line 3 col 2
        // Indices: 0, 1, 2
        // Line 0: 2..end (len 5 -> 2..5)
        // Line 1: 0..end (len 5 -> 0..5)
        // Line 2: 0..2

        val nodes = listOf(
            NodeInfo(null, Point(1, 2), Point(3, 2), null, null, null)
        )

        val result = getMergedHighlights(nodes, lines)

        assertEquals(3, result.size)
        assertEquals(listOf(2..5), result[0])
        assertEquals(listOf(0..5), result[1])
        assertEquals(listOf(0..2), result[2])
    }

    @Test
    fun `test out of bounds clamping`() {
        val lines = listOf("abc")
        // Node requests col 10..20 on line 1
        // Clamped to 3..3? No, startCol=10 coerced to 3. EndCol=20 coerced to 3.
        // Range 3..3.

        val nodes = listOf(
            NodeInfo(null, Point(1, 10), Point(1, 20), null, null, null)
        )

        val result = getMergedHighlights(nodes, lines)
        val ranges = result[0]!!
        assertEquals(listOf(3..3), ranges)
    }

    @Test
    fun `test invalid line number ignored`() {
        val lines = listOf("abc")
        // Node on line 100
        val nodes = listOf(
            NodeInfo(null, Point(100, 0), Point(100, 5), null, null, null)
        )

        // startLine = 99. coerceAtLeast(0) -> 99.
        // endLine = 99. coerceAtMost(0) -> 0.
        // loop 99..0 is empty.

        val result = getMergedHighlights(nodes, lines)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test mergeRanges logic explicitly`() {
        // Direct test of mergeRanges helper
        val ranges = listOf(0..5, 4..8, 10..12)
        val merged = mergeRanges(ranges)

        // 0..5 and 4..8 overlap -> 0..8
        // 10..12 is separate (gap 9)
        // Result: 0..8, 10..12

        assertEquals(2, merged.size)
        assertEquals(0..8, merged[0])
        assertEquals(10..12, merged[1])
    }

    @Test
    fun `test mergeRanges empty`() {
        val merged = mergeRanges(emptyList())
        assertTrue(merged.isEmpty())
    }
}
