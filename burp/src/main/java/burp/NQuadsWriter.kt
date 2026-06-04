package burp

import burp.model.*
import burp.vocabularies.BURP
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object NQuadsWriter {

    fun write(output: OutputStream, statements: List<RdfStatement>, charset: java.nio.charset.Charset = StandardCharsets.UTF_8) {
        val writer = output.writer(charset).buffered()
        for (statement in statements) {
            writer.append(serializeStatement(statement)).append('\n')
        }
        writer.flush()
    }

    internal fun serializeStatement(statement: RdfStatement): String {
        val subject = serializeSubject(statement.subject)
        val predicate = serializeIRI(statement.predicate)
        val obj = serializeObject(statement.`object`)
        val graph = serializeGraph(statement.graph)
        return if (graph == null) "$subject $predicate $obj ." else "$subject $predicate $obj $graph ."
    }

    private fun serializeSubject(subject: BlankNodeOrIRI): String = when (subject) {
        is IRITerm -> serializeIRI(subject)
        is BlankNodeTerm -> serializeBlankNode(subject)
        else -> throw IllegalArgumentException("Unsupported subject term $subject")
    }

    private fun serializeObject(obj: Term): String = when (obj) {
        is IRITerm -> serializeIRI(obj)
        is BlankNodeTerm -> serializeBlankNode(obj)
        is LiteralTerm -> serializeLiteral(obj)
        else -> throw IllegalArgumentException("Unsupported object term $obj")
    }

    private fun serializeGraph(graph: IRITerm?): String? {
        if (graph == null || graph.uri == BURP.defaultGraph.uri) return null
        return serializeIRI(graph)
    }

    private fun serializeIRI(iri: IRITerm): String = "<${iri.uri}>"

    private fun serializeBlankNode(blankNode: BlankNodeTerm): String {
        val label = if (blankNode.id.startsWith("_:")) blankNode.id else "_:${blankNode.id}"
        val normalizedLabel = normalizeBlankNodeLabel(label)
        return normalizedLabel
    }

    private fun isValidBlankNodeLabel(label: String): Boolean {
        // N-Quads BLANK_NODE_LABEL format: _: (PN_CHARS_U | [0-9]) ((PN_CHARS | '.')* PN_CHARS)?
        // Note: We're stricter than spec - we don't allow ':' or '.' in any position for simplicity.
        if (!label.startsWith("_:") || label.length < 3) return false

        val id = label.substring(2)
        if (id.isEmpty()) return false

        // First char must be letter, digit, or underscore
        val firstChar = id[0]
        if (!isValidFirstBlankNodeChar(firstChar)) return false

        // If length 1, it's valid
        if (id.length == 1) return true

        // Rest can only be valid encodable chars (no '.' or ':')
        for (i in 1 until id.length) {
            if (!isValidEncodableBlankNodeChar(id[i])) return false
        }

        return true
    }

    private fun isValidFirstBlankNodeChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_'
    }

    /**
     * Checks if a character is valid for use in a blank node label (positions after the first).
     *
     * Note: This is stricter than the N-Quads spec (BLANK_NODE_LABEL rule).
     * The spec allows PN_CHARS (which includes ':' via PN_CHARS_U) and '.' in middle positions,
     * with the constraint that '.' cannot be the final character.
     * For simplicity and performance, we exclude '.' and ':' entirely.
     */
    private fun isValidEncodableBlankNodeChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '-'
    }

    private fun normalizeBlankNodeLabel(label: String): String {
        return if (isValidBlankNodeLabel(label)) {
            label
        } else {
            // Encode invalid characters to keep label close to original
            val id = label.substring(2) // Remove "_:"
            val encoded = StringBuilder("_:")

            for (i in id.indices) {
                val char = id[i]
                if (isValidEncodableBlankNodeChar(char)) {
                    encoded.append(char)
                } else {
                    // Encode as _XX where XX is hex
                    encoded.append('_')
                    encoded.append(char.code.toString(16).padStart(2, '0'))
                }
            }

            encoded.toString()
        }
    }

    private fun serializeLiteral(literal: LiteralTerm): String {
        val lexicalForm = escapeLiteral(literal.value)
        val suffix = when {
            literal.language != null -> "@${literal.language}"
            literal.datatype != null && literal.datatype.uri != XSDstring.uri -> "^^${serializeIRI(literal.datatype)}"
            else -> ""
        }
        return "\"$lexicalForm\"$suffix"
    }

    internal fun escapeLiteral(value: String): String {
        val escaped = StringBuilder(value.length)
        for (char in value) {
            when (char) {
                '\\' -> escaped.append("\\\\")
                '"' -> escaped.append("\\\"")
                '\n' -> escaped.append("\\n")
                '\r' -> escaped.append("\\r")
                '\t' -> escaped.append("\\t")
                '\b' -> escaped.append("\\b")
                '\u000C' -> escaped.append("\\f")
                else -> {
                    if (char.code < 0x20) {
                        escaped.append("\\u")
                        escaped.append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        escaped.append(char)
                    }
                }
            }
        }
        return escaped.toString()
    }
}

