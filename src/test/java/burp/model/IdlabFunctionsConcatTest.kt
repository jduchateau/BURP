package burp.model

import burp.model.fnmlutil.IdlabConcatFunction
import burp.model.fnmlutil.IdlabConcatSequenceFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdlabFunctionsConcatTest {
    private val str = "https://w3id.org/imec/idlab/function#str"
    private val otherStr = "https://w3id.org/imec/idlab/function#otherStr"
    private val seq = "https://w3id.org/imec/idlab/function#seq"
    private val delimiter = "https://w3id.org/imec/idlab/function#delimiter"
    private val separator = "https://w3id.org/imec/idlab/function#separator"
    private val stringOut = "https://w3id.org/imec/idlab/function#_stringOut"

    @Test
    fun concatJoinsTwoStringsWithDelimiter() {
        val result = IdlabConcatFunction().apply(
            mapOf(
                str to LiteralTerm("hello"),
                otherStr to LiteralTerm("world"),
                delimiter to LiteralTerm("-")
            ),
            null
        ).single()

        assertEquals("hello-world", result.defaultValue)
        assertEquals("hello-world", result.get(stringOut, null))
    }

    @Test
    fun concatUsesSeparatorAlias() {
        val result = IdlabConcatFunction().apply(
            mapOf(
                str to LiteralTerm("a"),
                otherStr to LiteralTerm("b"),
                separator to LiteralTerm(":")
            ),
            null
        ).single()

        assertEquals("a:b", result.defaultValue)
    }

    @Test
    fun concatSequenceJoinsRdfSeqElementsWithDelimiter() {
        val sequence = RdfSeqTerm(
            mutableListOf(LiteralTerm("x"), LiteralTerm("y"), LiteralTerm("z")),
            BlankNodeTerm("seq1"),
            idGenerated = true
        )

        val result = IdlabConcatSequenceFunction().apply(
            mapOf(seq to sequence, delimiter to LiteralTerm("|")),
            null
        ).single()

        assertEquals("x|y|z", result.defaultValue)
        assertEquals("x|y|z", result.get(stringOut, null))
    }

    @Test
    fun concatSequenceReturnsNullForEmptySequence() {
        val sequence = RdfSeqTerm(mutableListOf(), BlankNodeTerm("seq2"), idGenerated = true)

        val result = IdlabConcatSequenceFunction().apply(mapOf(seq to sequence), null).single()

        assertEquals(null, result.defaultValue)
        assertEquals(null, result.get(stringOut, null))
    }
}

