package burp.reporting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import turtleprov.Point

class FileGeometryTest {

    @Test
    fun `test user reproduction case with merged ranges`() {
        val lines = """@prefix ex: <http://example.com/> .
            |@prefix rml: <http://w3id.org/rml/> .
            |<http://example.com/base/TriplesMap1> a rml:TriplesMap;
            |
            |rml:logicalSource [ a rml:LogicalSource;
            |   rml:iterator ""$.students[*]"";
            |   rml:referenceFormulation rml:JSONPath;
            |   rml:source [ a rml:RelativePathSource;
            |       rml:root rml:MappingDirectory;
            |       rml:path "student2.json" 
            |   ]
            |] .
            |""".trimMargin().lines()

        val nodes = listOf(
            PointRange(Point(5, 18), Point(5, 20)),
            PointRange(Point(5, 21), Point(5, 31)),
            PointRange(Point(9, 7), Point(9, 14)),
            PointRange(Point(9, 17), Point(9, 29))
        )

        val result = getMergedHighlights(nodes, lines)

        assertTrue(result.containsKey(5))
        assertTrue(result.containsKey(9))

        val line6 = result[5]!!
        assertEquals(1, line6.size)
        assertEquals(18..31, line6[0])

        val line10 = result[9]!!
        assertEquals(2, line10.size)
        assertEquals(7..14, line10[0])
        assertEquals(17..29, line10[1])
    }

    @Test
    fun `test overlapping ranges merge`() {
        val lines = listOf("0123456789")
        // Overlapping: 0..4 and 2..6 -> 0..6
        val nodes = listOf(
            PointRange(Point(0, 0), Point(0, 4)),
            PointRange(Point(0, 2), Point(0, 6))
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
            PointRange(Point(0, 0), Point(0, 4)),
            PointRange(Point(0, 5), Point(0, 9))
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
            PointRange(Point(0, 0), Point(0, 3)),
            PointRange(Point(0, 5), Point(0, 8))
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
            PointRange(Point(0, 2), Point(2, 2))
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
            PointRange(Point(0, 10), Point(0, 20))
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
            PointRange(Point(100, 0), Point(100, 5))
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
