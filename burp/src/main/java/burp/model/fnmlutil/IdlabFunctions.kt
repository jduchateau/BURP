package burp.model.fnmlutil

import burp.model.LiteralTerm
import burp.model.RdfSeqTerm
import burp.reporting.Origin
import com.google.auto.service.AutoService
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Any?.toValueString(): String? = when (this) {
    is LiteralTerm -> this.value
    null -> null
    else -> this.toString()
}

@AutoService(RMLFunction::class)
class IdlabToUpperCaseURLFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#toUpperCaseURL"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val strParam = parameters["https://w3id.org/imec/idlab/function#str"]
        val strUpper = strParam.toValueString()?.uppercase(Locale.getDefault())

        // FIXME Check if all parts of the URL are valid
        val out = if (strUpper == null || strUpper.startsWith("HTTP://")) strUpper else "http://$strUpper"
        val r = Return(out, "https://w3id.org/imec/idlab/function#_stringOut" to out)
        return listOf(r)
    }

}

@OptIn(ExperimentalUuidApi::class)
@AutoService(RMLFunction::class)
class IdlabRandomFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#random"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val out = Uuid.random().toString()
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
class IdlabConcatFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#concat"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val str = parameters["https://w3id.org/imec/idlab/function#str"].toValueString()
        val otherStr = parameters["https://w3id.org/imec/idlab/function#otherStr"].toValueString()
        val delimiter = parameters["https://w3id.org/imec/idlab/function#delimiter"].toValueString()
            ?: parameters["https://w3id.org/imec/idlab/function#separator"].toValueString()
            ?: ""

        val out = listOf(str, otherStr).filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(delimiter)
        val r = Return(out, "https://w3id.org/imec/idlab/function#_stringOut" to out)
        return listOf(r)
    }
}

@AutoService(RMLFunction::class)
class IdlabConcatSequenceFunction : RMLFunction {
    override val name = "https://w3id.org/imec/idlab/function#concatSequence"
    override fun apply(parameters: Map<String, Any?>, origin: Origin?): List<Return> {
        val delimiter = parameters["https://w3id.org/imec/idlab/function#_delimiter"].toValueString() ?: ""

        val seq = when (val raw = parameters["https://w3id.org/imec/idlab/function#_seq"]) {
            is RdfSeqTerm -> raw.elements
            is Iterable<*> -> raw.toList()
            is Array<*> -> raw.toList()
            null -> emptyList()
            else -> listOf(raw)
        }

        val stringValues = seq.mapNotNull { it.toValueString() }
        val out = stringValues.joinToString(delimiter)
        val r = Return(out, "https://w3id.org/imec/idlab/function#_stringOut" to out)
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
            is LiteralTerm -> condition.booleanOrNull()
            else -> condition?.toString()?.toBoolean() ?: false
        }

        return if (isTrue != null && isTrue) {
            listOf(Return(expr))
        } else {
            emptyList()
        }
    }
}
