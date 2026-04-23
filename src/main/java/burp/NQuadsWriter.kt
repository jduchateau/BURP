package burp

import burp.model.BlankNodeOrIRI
import burp.model.BlankNodeTerm
import burp.model.IRITerm
import burp.model.LiteralTerm
import burp.model.RdfStatement
import burp.model.Term
import burp.vocabularies.RML
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object NQuadsWriter {

    fun write(output: OutputStream, statements: List<RdfStatement>) {
        val writer = output.writer(StandardCharsets.UTF_8).buffered()
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
        if (graph == null || graph.uri == RML.defaultGraph.uri) return null
        return serializeIRI(graph)
    }

    private fun serializeIRI(iri: IRITerm): String = "<${iri.uri}>"

    private fun serializeBlankNode(blankNode: BlankNodeTerm): String {
        return if (blankNode.id.startsWith("_:")) blankNode.id else "_:${blankNode.id}"
    }

    private fun serializeLiteral(literal: LiteralTerm): String {
        val lexicalForm = escapeLiteral(literal.value)
        val suffix = when {
            literal.language != null -> "@${literal.language}"
            literal.datatype != null -> "^^${serializeIRI(literal.datatype)}"
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

