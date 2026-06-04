package burp.model.deciders

import burp.ls.CSVSource
import burp.ls.JSONSourceRFC
import burp.ls.RDBSource
import burp.ls.XMLSource
import burp.vocabularies.Rml
import rdf.DatasetCore
import rdf.Term
import rdfobjectloader.TypeDecider
import kotlin.reflect.KClass

class LogicalSourceTypeDecider : TypeDecider {
    override fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>> {
        if (targetClass != burp.model.AbstractLogicalSource::class) return emptySet()
        
        val viewOnProp = rdfkt.NamedTerm("http://w3id.org/rml/viewOn")
        if (dataset.match(subject = resource, predicate = viewOnProp).any()) {
            return setOf(burp.model.lv.LogicalView::class)
        }
        
        val referenceFormulationProp = rdfkt.NamedTerm(Rml.referenceFormulation)
        val formulationQuads = dataset.match(subject = resource, predicate = referenceFormulationProp)
        if (formulationQuads.any()) {
            val refFormulation = formulationQuads.first().`object`.value
            return when (refFormulation) {
                "http://semweb.mmlab.be/ns/ql#CSV" -> setOf(CSVSource::class)
                "http://semweb.mmlab.be/ns/ql#JSONPath" -> setOf(JSONSourceRFC::class)
                "http://semweb.mmlab.be/ns/ql#XPath" -> setOf(XMLSource::class)
                else -> emptySet()
            }
        }
        
        // If it has rml:source and it's a D2RQ database or similar, return RDBSource
        val sourceProp = rdfkt.NamedTerm(Rml.source)
        val sourceQuads = dataset.match(subject = resource, predicate = sourceProp)
        if (sourceQuads.any()) {
            val sourceObj = sourceQuads.first().`object`
            val rdfType = rdfkt.NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
            val typeQuads = dataset.match(subject = sourceObj, predicate = rdfType)
            if (typeQuads.any { it.`object`.value.startsWith("http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ") }) {
                return setOf(RDBSource::class)
            }
        }
        
        return emptySet()
    }
}
