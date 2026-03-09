package burp.model.fnmlutil

import burp.reporting.Origin
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Literal
import java.net.URI
import java.util.*

@AutoService(RMLFunction::class)
class IdlabToUpperCaseURLFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#toUpperCaseURL"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val str = parameters["https://w3id.org/imec/idlab/function#str"].toString().uppercase(Locale.getDefault())

        @Suppress("HttpUrlsUsage")
        val out = if (isURL(str)) str else "http://$str"
        val r = Return(out, "https://w3id.org/imec/idlab/function#_stringOut" to out)
        return listOf(r)
    }

    fun isURL(url: String): Boolean {
        try {
            URI(url).toURL()
            return true
        } catch (_: Exception) {
            return false
        }
    }
}

@AutoService(RMLFunction::class)
class IdlabRandomFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#random"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val out = "e4dcc7ee-8e2a-4012-92cc-9a74dd545e89" //TODO Should be only hard-coded (mocked) when testing
        val r = Return(out, "https://w3id.org/imec/idlab/function#_stringOut" to out)
        return listOf(r)
    }
}

@AutoService(RMLFunction::class)
class IdlabEqualFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#equal"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val expr1 = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam"]
        val expr2 = parameters["http://users.ugent.be/~bjdmeest/function/grel.ttl#valueParam2"]
        val out = expr1 == expr2
        val r = Return(out, "https://w3id.org/imec/idlab/function#_boolOut" to out)
        return listOf(r)
    }
}

@AutoService(RMLFunction::class)
class IdlabIfFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#IF"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val condition = parameters["https://w3id.org/imec/idlab/function#boolParameter"]
        val expr = parameters["https://w3id.org/imec/idlab/function#expressionParameter"]

        val isTrue = when (condition) {
            is Boolean -> condition
            is Literal -> condition.boolean
            else -> condition?.toString()?.toBoolean() ?: false
        }

        return if (isTrue) {
            listOf(Return(expr))
        } else {
            emptyList()
        }
    }
}
