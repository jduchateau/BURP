package burp.util

import org.apache.jena.atlas.io.OutputUtils
import org.apache.jena.iri.IRIFactory
import org.apache.jena.iri.ViolationCodes
import org.apache.jena.rfc3986.Chars3986
import java.util.*

/**
 * Translate a string into its IRI safe value as per RML's steps:
 * percent-encode
 * 
 * @param string
 * @return
 */
fun toIRISafe(string: String): String {
    // The IRI-safe version of a string is obtained by applying the following 
    // transformation to any character that is not in the iunreserved 
    // production in [RFC3987].
    val sb = StringBuffer()
    for (c in string.toCharArray()) {
        if (Chars3986.iunreserved(c)) sb.append(c)
        else sb.append('%'.toString() + Integer.toHexString(c.code).uppercase(Locale.getDefault()))
    }
    return sb.toString()
}

/**
 * Translate a string into its URI safe value as per RML's steps:
 * 1. UTF-8 encode
 * 2. percent-encode
 * 
 * @param string
 * @return
 */
fun toURISafe(string: String): String {
    // The IRI-safe version of a string is obtained by applying the following 
    // transformation to any character that is not in the iunreserved 
    // production in [RFC3987].
    val sb = StringBuilder()
    val bytes = string.toByteArray(Charsets.UTF_8)
    for (b in bytes) {
        val c = b.toInt().toChar()
        if (Chars3986.unreserved(c)) {
            sb.append(c)
        } else {
            sb.append("%")
            OutputUtils.printHex(sb, b.toInt() and 0xFF, 2)
        }
    }
    return sb.toString()
}

/**
 * Converts a byte array into a Hex string
 * Code based on https://www.programiz.com/java-programming/examples/convert-byte-array-hexadecimal
 */
private val hexArray = "0123456789ABCDEF".toCharArray()
fun bytesToHexString(o: ByteArray?): String {
    val bytes = o as ByteArray
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
        val v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}


fun isValidAndAbsoluteIRI(string: String): Boolean =
    isValidAndAbsolute(string, IRIFactory.iriImplementation())

fun isValidAndAbsoluteURI(string: String): Boolean =
    isValidAndAbsolute(string, IRIFactory.uriImplementation())

private fun isValidAndAbsolute(string: String, factory: IRIFactory): Boolean {
    val iri = factory.create(string)
    val hasViolations = iri.violations(false).asSequence()
        // TODO: We ignore CAPS in HOST for test cases, but we shouldn't
        .any { it.violationCode != ViolationCodes.LOWERCASE_PREFERRED }
    return !hasViolations && !iri.scheme.isNullOrBlank()

    // FIXME: Investigate which definition of Absolute IRI/URI we need:
    //  Jena uses the first one for iri.isAbsolute but RML require fragments to pass
    //  for example http://www.w3.org/2001/XMLSchema#string
    //    Definition from RFC3986 section 4.3
    //    return has(SCHEME) && !has(FRAGMENT);
    //    Definition from RFC2396 section 3.1
    //    return has(SCHEME);
}


