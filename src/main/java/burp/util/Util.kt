package burp.util

import org.apache.jena.iri.IRIFactory
import org.apache.jena.iri.ViolationCodes
import java.util.*

/**
 * Translate a string into its IRI safe value as per R2RML's steps
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
        if (inIUNRESERVED(c)) sb.append(c)
        else sb.append('%'.toString() + Integer.toHexString(c.code).uppercase(Locale.getDefault()))
    }
    return sb.toString()
}

/**
 * Check whether the characters are part of iunreserved as per
 * https://tools.ietf.org/html/rfc3987#section-2.2
 */
private fun inIUNRESERVED(c: Char): Boolean {
    if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~".indexOf(c) != -1) return true
    else if (c.code in 160..55295) return true
    else if (c.code in 63744..64975) return true
    else if (c.code in 65008..65519) return true
    else if (c.code in 65536..131069) return true
    else if (c.code in 131072..196605) return true
    else if (c.code in 196608..262141) return true
    else if (c.code in 262144..327677) return true
    else if (c.code in 327680..393213) return true
    else if (c.code in 393216..458749) return true
    else if (c.code in 458752..524285) return true
    else if (c.code in 524288..589821) return true
    else if (c.code in 589824..655357) return true
    else if (c.code in 655360..720893) return true
    else if (c.code in 720896..786429) return true
    else if (c.code in 786432..851965) return true
    else if (c.code in 851968..917501) return true
    else if (c.code in 921600..983037) return true
    return false
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


