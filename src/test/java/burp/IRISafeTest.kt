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
        assertEquals("~A_17.1-2_¢.€.\uD83C\uDF0D", toIRISafe("~A_17.1-2_¢.€.\uD83C\uDF0D"))
    }

    @Test
    fun testToURISafe_SpecialChars() {
        assertEquals("~A_17.1-2_%C2%A2.%E2%82%AC.%F0%9F%8C%8D", toURISafe("~A_17.1-2_¢.€.\uD83C\uDF0D"))
    }

    @Test
    fun testToURISafe_Zoe() {
        assertEquals("Zo%C3%AB%20Kr%C3%BCger", toURISafe("Zoë Krüger"))
    }

    @Test
    fun testToURISafe_Cyrilic() {
        assertEquals("%D1%88%D0%B5%D0%BB%D0%BB%D1%8B", toURISafe("шеллы"))
    }

    @Test
    fun testToIRISafe_Zoe() {
        assertEquals("Zoë%20Krüger", toIRISafe("Zoë Krüger"))
    }
}
