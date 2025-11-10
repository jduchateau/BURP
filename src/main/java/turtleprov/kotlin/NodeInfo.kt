package turtleprov.kotlin

import rdf.Quad
import org.antlr.v4.kotlinruntime.ast.Point

/**
 * Contains information about a node's kind and position
 */
data class NodeInfo(
    val kind: TurtleNodeKind,
    val start: Point?,
    val end: Point?,
    val blankNodeId: String? = null // For blank nodes to enable reconstruction
)

data class ProvQuad(
    val quad: Quad,
    val subjectInfo: NodeInfo?,
    val predicateInfo: NodeInfo?,
    val objectInfo: NodeInfo?,
) {
    constructor(quad: Quad) : this(quad, null, null, null)
}


class ProvStore(
    val quads: MutableSet<ProvQuad> = HashSet(),
    val prefixes: MutableMap<String, String> = mutableMapOf()
)