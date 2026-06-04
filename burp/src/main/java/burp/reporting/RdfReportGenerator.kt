package burp.reporting

import burp.Main
import burp.vocabularies.PTR
import burp.vocabularies.RER
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages.filenameToLang
import rdfobjectloader.LiteralPart
import rdfobjectloader.StatementParts
import turtleprov.jena
import java.io.FileOutputStream
import java.util.*


fun generateRdfReport(report: RmlExecutionReport, outputFile: String) {
    val model = ModelFactory.createDefaultModel()
    val reportRdf = model.createResource(RER.RmlExecutionReport)

    // Load processor info from Maven-filtered properties
    val props = Properties()
    val propsFile = Main::class.java.getResourceAsStream("/burp.properties")
    if (propsFile != null) props.load(propsFile)

    val processorName = props.getProperty("processor.name", null)
    val processorVersion = props.getProperty("processor.version", null)

    reportRdf.addProperty(RER.processorName, processorName)
    reportRdf.addProperty(RER.processorVersion, processorVersion)

    reportRdf.addProperty(
        RER.generatedStatements,
        model.createTypedLiteral(report.statistics.generatedStatements)
    )
    for ((tm, count) in report.statistics.generatedStatementPerTriplesMap) {
        val bn = model.createResource(RER.GeneratedStatementPerTriplesMap)
        val subjectTerm = tm.subject
        if (subjectTerm != null) {
            val subjectRes = when (subjectTerm) {
                is rdfkt.JenaNamedNode -> subjectTerm.node
                is rdfkt.JenaBlankNode -> subjectTerm.node
                is rdf.NamedNode -> org.apache.jena.rdf.model.ResourceFactory.createResource(subjectTerm.value)
                is rdf.BlankNode -> model.createResource(org.apache.jena.rdf.model.AnonId(subjectTerm.value))
                else -> org.apache.jena.rdf.model.ResourceFactory.createResource(subjectTerm.value)
            }
            bn.addProperty(RER.triplesMap, subjectRes)
        }
        bn.addProperty(RER.generatedStatements, model.createTypedLiteral(count))
        reportRdf.addProperty(RER.generatedStatementsPerTriplesMap, bn)
    }

    report.errors.forEach { addRmlError(model, reportRdf, it) }

    val lang = filenameToLang(outputFile) ?: Lang.NT
    RDFDataMgr.write(FileOutputStream(outputFile), model, lang)
}

fun addRmlError(
    model: Model,
    reportResource: Resource,
    error: RmlError
) {
    val errorResource = model.createResource(error.errorType)
    model.add(reportResource, RER.hasError, errorResource)
    errorResource.addProperty(RER.message, error.message)
    error.exception?.let { ex ->
        errorResource.addProperty(RER.stackTrace, ex.stackTraceToString())
    }

    error.context.forEach { (prop, value) ->
        errorResource.addProperty(prop, value as? Resource ?: model.createTypedLiteral(value))
    }


    // Adds origin metadata to the error resource
    error.origin?.let { origin ->
        origin.planNode.let { errorResource.addProperty(RER.planNode, it.toString()) }
        origin.sourceStatements?.forEach { ptr ->
            when (ptr) {
                is StatementParts -> {
                    val sp = model.createResource(PTR.StatementPart)
                    sp.addProperty(PTR.statement, model.createStatementTerm(ptr.stmt.jena()))
                    if (ptr.subject) sp.addProperty(PTR.part, PTR.Subject)
                    if (ptr.predicate) sp.addProperty(PTR.part, PTR.Predicate)
                    if (ptr.`object`) sp.addProperty(PTR.part, PTR.Object)
                    model.add(errorResource, RER.mappingStatement, sp)
                }

                is LiteralPart -> {
                    val lp = model.createResource(PTR.StatementPart)
                    lp.addProperty(PTR.statement, model.createStatementTerm(ptr.stmt.jena()))
                    lp.addProperty(PTR.part, PTR.Object)

                    val range = model.createResource(PTR.Range)
                    lp.addProperty(PTR.textRange, range)

                    val startPoint = model.createResource(PTR.Point)
                    startPoint.addProperty(PTR.line, model.createTypedLiteral(ptr.objectRange.start.line))
                    startPoint.addProperty(PTR.column, model.createTypedLiteral(ptr.objectRange.start.column))
                    range.addProperty(PTR.start, startPoint)

                    val objectEnd = ptr.objectRange.end
                    if (objectEnd != null) {
                        val endPoint = model.createResource(PTR.Point)
                        endPoint.addProperty(PTR.line, model.createTypedLiteral(objectEnd.line))
                        endPoint.addProperty(PTR.column, model.createTypedLiteral(objectEnd.column))
                        range.addProperty(PTR.end, endPoint)
                    }
                }
            }
        }
    }
}
