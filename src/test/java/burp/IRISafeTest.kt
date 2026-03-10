package burp

import burp.util.toIRISafe
import burp.util.toURISafe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IRISafeTest {

    @Test
    fun testToIRISafe_Numeric() {
        assertEquals("42", toIRISafe("42"))
    }

    @Test
    fun testToIRISafe_HelloWorld() {
        assertEquals("Hello%20World%21", toIRISafe("Hello World!"))
    }

    @Test
    fun testToIRISafe_DateTime() {
        assertEquals("2011-08-23T22%3A17%3A00Z", toIRISafe("2011-08-23T22:17:00Z"))
    }

    @Test
    fun testToIRISafe_SpecialChars() {
        assertEquals("~A_17.1-2", toIRISafe("~A_17.1-2"))
    }

    @Test
    fun testToIRISafe_ZoeURI() {
        assertEquals("Zo%C3%AB%20Kr%C3%BCger",toURISafe("Zoë Krüger") )
    }

    @Test
    fun testToIRISafe_ZoeIRI() {
        assertEquals("Zoë%20Krüger",toIRISafe("Zoë Krüger") )
    }
}
