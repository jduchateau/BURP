package turtleprov

import org.apache.jena.datatypes.TypeMapper
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.*
import rdfkt.*
import rdfkt.Literal
import java.io.File

private fun Term.toJenaNode(model: Model): RDFNode = when (this) {
    is NamedTerm -> model.createResource(this.value)
    is BlankTerm -> model.createResource(AnonId(this.value))
    is Literal -> {
        val currentLang = this.lang
        val currentType = this.type
        when {
            currentLang != null -> model.createLiteral(this.value, currentLang)
            currentType != null -> {
                val rdfType = TypeMapper.getInstance().getSafeTypeByName(currentType.value)
                model.createTypedLiteral(this.value, rdfType)
            }

            else -> model.createLiteral(this.value)
        }
    }

    is Quad -> {
        model.createStatementTerm(this.toJenaStatement(model))
    }

    else -> throw IllegalArgumentException("Unsupported term type: $this")
}

private fun Quad.toJenaStatement(model: Model): Statement {
    val subj = this.s.toJenaNode(model).asResource()
    val pred = model.createProperty(this.p.value)
    val obj = this.o.toJenaNode(model)
    return model.createStatement(subj, pred, obj)
}

fun parseTurtleFromFile(turtleFile: File): Dataset {
    val store: ProvStore = parseTurtleFromFile(turtleFile.absolutePath)
    val quads = RDF12Converter().toQuads(store)

    val model = ModelFactory.createDefaultModel()
    for ((prefix, uri) in store.prefixes) {
        model.setNsPrefix(prefix, uri)
    }

    for (quad in quads) {
        model.add(quad.toJenaStatement(model))
    }

    return DatasetFactory.create(model)
}