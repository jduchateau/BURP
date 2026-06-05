package burp.model

import burp.model.fnmlutil.StringSha1Function
import burp.model.fnmlutil.StringSha256Function
import burp.reporting.BurpException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GrelFunctionsHashTest {
    private val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
    private val stringOut = "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut"

    @Test
    fun stringSha1HashesValue() {
        val result = StringSha1Function().apply(mapOf(valueParam to "hello"), null).single()

        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", result.defaultValue)
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", result.get(stringOut, null))
    }

    @Test
    fun stringSha256HashesValue() {
        val result = StringSha256Function().apply(mapOf(valueParam to "hello"), null).single()

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result.defaultValue)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result.get(stringOut, null))
    }

    @Test
    fun stringSha1ThrowsOnMissingParameter() {
        assertThrows(BurpException::class.java) {
            StringSha1Function().apply(emptyMap(), null)
        }
    }
}
