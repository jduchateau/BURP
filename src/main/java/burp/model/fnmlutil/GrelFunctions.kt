package burp.model.fnmlutil

import burp.Main
import burp.model.LiteralTerm
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import com.google.auto.service.AutoService
import org.apache.commons.lang3.Strings
import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.text.WordUtils
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.XSD
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

@AutoService(RMLFunction::class)
class BooleanAndFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#boolean_and"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val a = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool_a"] as Literal
        val b = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool_b"] as Literal
        val out = a.boolean && b.boolean
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class BooleanNotFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#boolean_not"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val a = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool"] as Literal
        val out = !a.boolean
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class BooleanOrFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#boolean_or"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val a = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool_a"] as Literal
        val b = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool_b"] as Literal
        val out = a.boolean || b.boolean
        val re = Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out)
        return listOf(re)
    }
}

@AutoService(RMLFunction::class)
class BooleanXorFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#boolean_xor"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val a = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool_a"] as Literal
        val b = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_bool_b"] as Literal
        val out = a.boolean xor b.boolean
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class StringChompFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_chomp"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val f = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_string_sep"].toString()
        val out = Strings.CS.removeEnd(s, f)
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class StringContainsFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_contains"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val f = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_string_sub"].toString()
        val out = s.contains(f)
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_bool" to out))
    }
}

@AutoService(RMLFunction::class)
class StringContainsPatternFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_contains_pattern"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val p = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_regex"].toString()
        val out = s.matches(p.toRegex())
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_bool" to out))
    }
}

@AutoService(RMLFunction::class)
class EndsWithFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#endsWith"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val f = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_string_sub"].toString()
        val out = s.endsWith(f)
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_bool" to out))
    }
}

// Escapes s in the given escaping mode. The mode can be one of: "html", "xml", "csv", "url", "javascript".
// Note that quotes are required around your mode. See the recipes for examples of escaping and unescaping.
@AutoService(RMLFunction::class)
class EscapeFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#escape"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toValueString()
        val p = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#modeParam"].toValueString()
        val modeLower = p?.lowercase(Locale.getDefault())
        val out = when (modeLower) {
            "html" -> StringEscapeUtils.escapeHtml4(s)
            "xml" -> StringEscapeUtils.escapeXml11(s)
            "csv" -> StringEscapeUtils.escapeCsv(s)
            "javascript" -> StringEscapeUtils.escapeEcmaScript(s)
            "url" -> URLEncoder.encode(s, StandardCharsets.UTF_8)
            else -> throw RMLFunctionException(
                String.format("Mode %s not supported in GREL's escape function.", p),
                Exception(),
                this
            )
        }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class LengthFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_length"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toValueString()
        val out = s?.length
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_number" to out))
    }
}

@AutoService(RMLFunction::class)
class MathAbsFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#math_abs"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_dec_n"] as Literal
        var out: Any? = null
        val duri = s.datatypeURI
        out = when {
            XSD.integer.uri == duri || XSD.xint.uri == duri -> abs(s.lexicalForm.toInt())
            XSD.xdouble.uri == duri -> abs(s.lexicalForm.toDouble())
            XSD.xlong.uri == duri -> abs(s.lexicalForm.toLong())
            XSD.xfloat.uri == duri -> abs(s.lexicalForm.toFloat())
            XSD.xshort.uri == duri -> abs(s.lexicalForm.toShort().toInt())
            else -> throw RMLFunctionException(
                String.format("Mode %s not supported in GREL's abs function.", s),
                Exception(),
                this
            )
        }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_decimal" to out))
    }
}

@AutoService(RMLFunction::class)
class MathCeilFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#math_ceil"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_dec_n"] as Literal
        var out: Any? = null
        val duri = s.datatypeURI
        out = when {
            XSD.integer.uri == duri || XSD.xint.uri == duri -> ceil(s.lexicalForm.toInt().toDouble()).toInt()
            XSD.xdouble.uri == duri -> ceil(s.lexicalForm.toDouble()).toInt()
            XSD.xlong.uri == duri -> ceil(s.lexicalForm.toLong().toDouble()).toInt()
            XSD.xfloat.uri == duri -> ceil(s.lexicalForm.toFloat().toDouble()).toInt()
            XSD.xshort.uri == duri -> ceil(s.lexicalForm.toShort().toDouble()).toInt()
            else -> throw RMLFunctionException(
                String.format("Mode %s not supported in GREL's ceil function.", s),
                Exception(),
                this
            )
        }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_number" to out))
    }
}

@AutoService(RMLFunction::class)
class MathFloorFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#math_floor"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_dec_n"] as Literal
        var out: Any? = null
        val duri = s.datatypeURI
        out = when (duri) {
            XSD.integer.uri, XSD.xint.uri -> floor(s.lexicalForm.toInt().toDouble()).toInt()
            XSD.xdouble.uri -> floor(s.lexicalForm.toDouble()).toInt()
            XSD.xlong.uri -> floor(s.lexicalForm.toLong().toDouble()).toInt()
            XSD.xfloat.uri -> floor(s.lexicalForm.toFloat().toDouble()).toInt()
            XSD.xshort.uri -> floor(s.lexicalForm.toShort().toDouble()).toInt()
            else -> throw RMLFunctionException(
                String.format("Mode %s not supported in GREL's floor function.", s),
                Exception(),
                this
            )
        }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_number" to out))
    }
}

@AutoService(RMLFunction::class)
class StartsWithFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#startsWith"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val f = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_string_sub"].toString()
        val out = s.startsWith(f)
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#output_bool" to out))
    }
}

@AutoService(RMLFunction::class)
class StringGetFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_get"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val from = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#p_int_i_from"] as Literal
        val to = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#p_int_i_opt_to"] as Literal?
        var out: String? = null
        val f = from.int
        if (to != null) {
            val t = to.int
            if (t > 0) out = s.substring(f, t)
            else out = s.substring(f, s.length + t)
        } else out = s.substring(f, f + 1)
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class StringReplaceFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_replace"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toValueString()
        val f = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_find"].toValueString()
        val r = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#param_replace"].toValueString()
        val regex = f?.toRegex()
        val out = regex?.let { s?.replace(it, r ?: "") }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class StringStripFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_strip"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val out = s.trim { it <= ' ' }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class StringSubstringFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_substring"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        val string = parameters[valueParam].toValueString() ?: throw BurpException(
            RmlError(
                "Missing parameter $valueParam in string_substring, received: ${parameters[valueParam]}",
                origin,
                RER.FunctionExecutionError
            )
        )
        val pIntIFrom = "http://users.ugent.be/~bjdmeest/function/grel.ttl#p_int_i_from"
        val from = (parameters[pIntIFrom] as LiteralTerm).intOrNull() ?: throw BurpException(
            RmlError(
                "Missing or invalid parameter $pIntIFrom in string_substring, received: ${parameters[pIntIFrom]}",
                origin,
                RER.FunctionExecutionError
            )
        )
        val pIntIOptTo = "http://users.ugent.be/~bjdmeest/function/grel.ttl#p_int_i_opt_to"
        val to = (parameters[pIntIOptTo] as LiteralTerm?)?.intOrNull()
        var out: String? = null
        try {
            out = if (to != null) {
                if (to > 0) string.substring(from, to)
                else string.substring(from, string.length + to)
            } else string.substring(from)
        } catch (e: StringIndexOutOfBoundsException) {
            Main.report.errors.add(
                RmlError(
                    "String index out of bounds [$from, ${to ?: "null"}] in string (length ${string.length}) $string",
                    origin,
                    RER.FunctionExecutionError,
                    exception = e,
                    context = buildMap {
                        put(ResourceFactory.createProperty(valueParam), string)
                        put(ResourceFactory.createProperty(pIntIFrom), from)
                        if (to != null) put(ResourceFactory.createProperty(pIntIOptTo), to)
                    }
                )
            )
        }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))

    }
}

@AutoService(RMLFunction::class)
class StringTrimFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_trim"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"].toString()
        val out = s.trim { it <= ' ' }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class ToLowerCaseFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#toLowerCase"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"]?.toString()
        val out = s?.lowercase(Locale.getDefault())
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class ToUpperCaseFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#toUpperCase"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        val string = parameters[valueParam].toValueString()
        if (string == null) {
            Main.report.errors.add(
                RmlError(
                    "Missing parameter $valueParam in $name function, received: ${parameters}",
                    origin,
                    RER.FunctionExecutionError
                )
            )
        }
        val out = string?.uppercase(Locale.getDefault())
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))

    }
}

@AutoService(RMLFunction::class)
class ToTitleCaseFunction : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#toTitleCase"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {

        val s = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"]?.toString()
        val out = s?.let { WordUtils.capitalizeFully(it.lowercase(Locale.getDefault())) }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))

    }
}

private fun hashWithAlgorithm(value: String, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm).digest(value.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

@AutoService(RMLFunction::class)
class StringSha1Function : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_sha1"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        val string = getRequiredParameter(parameters, valueParam, origin).toValueString()
        val out = string?.let { hashWithAlgorithm(it, "SHA-1") }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class StringSha256Function : RMLFunction {
    override val name = "http://users.ugent.be/~bjdmeest/function/grel.ttl#string_sha256"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        val string = getRequiredParameter(parameters, valueParam, origin).toValueString()
        val out = string?.let { hashWithAlgorithm(it, "SHA-256") }
        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}
