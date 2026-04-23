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
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@AutoService(RMLFunction::class)
class HelloWorldFunction : RMLFunction {
    override val name = "http://example.com/functions/helloworld"

    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        return listOf(Return("Hello World!"))
    }
}

@AutoService(RMLFunction::class)
class SchemaFunction : RMLFunction {
    override val name = "http://example.com/functions/schema"

    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://example.com/functions/stringParameter"].toString()
        val out = "https://schema.org/$s"

        return listOf(Return(out, "http://example.com/functions/stringOutput" to out))
    }
}

@AutoService(RMLFunction::class)
class ParseURL : RMLFunction {
    override val name = "http://example.com/functions/parseURL"

    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val s = parameters["http://example.com/functions/stringParameter"].toString()
        val l: MutableList<Return> = ArrayList<Return>()
        val url = URI(s).toURL()

        val protocol = url.protocol
        val domain = url.host
        val path = url.path

        val r = Return(
            path,
            "http://example.com/functions/stringOutput" to path,
            "http://example.com/functions/protocolOutput" to protocol,
            "http://example.com/functions/domainOutput" to domain
        )
        l.add(r)

        return l
    }
}


@OptIn(ExperimentalUuidApi::class)
@AutoService(RMLFunction::class)
class UUIDFunction : RMLFunction {
    override val name = "https://github.com/morph-kgc/morph-kgc/function/built-in.ttl#uuid"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        return listOf(Return(Uuid.random().toString()))
    }
}


@AutoService(RMLFunction::class)
class ToSafeIRIFunction : RMLFunction {
    override val name = "http://BURP.noname/function/toSafeIRI"

    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        val string = getRequiredParameter(parameters, valueParam, origin).toValueString()
        val out = string?.let { burp.util.toIRISafe(it) }

        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

@AutoService(RMLFunction::class)
class ToSafeURIFunction : RMLFunction {
    override val name = "http://BURP.noname/function/toSafeURI"

    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val valueParam = "http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"
        val string = getRequiredParameter(parameters, valueParam, origin).toValueString()
        val out = string?.let { burp.util.toURISafe(it) }

        return listOf(Return(out, "http://users.ugent.be/~bjdmeest/function/grel.ttl#stringOut" to out))
    }
}

