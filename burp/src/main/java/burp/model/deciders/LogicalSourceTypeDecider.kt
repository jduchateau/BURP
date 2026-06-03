package burp.model.deciders

import burp.ls.CSVSource
import burp.ls.JSONSourceRFC
import burp.ls.RDBSource
import burp.ls.XMLSource
import burp.vocabularies.Rml
import org.apache.jena.rdf.model.ResourceFactory
import rdf.DatasetCore
import rdf.Term
import rdfkt.JenaDataset
import rdfkt.JenaNamedNode
import rdfobjectloader.TypeDecider
import kotlin.reflect.KClass

class LogicalSourceTypeDecider : TypeDecider {
    override fun decide(dataset: DatasetCore, resource: Term, targetClass: KClass<*>): Set<KClass<*>> {
        if (targetClass != burp.model.AbstractLogicalSource::class) return emptySet()
        val model = (dataset as JenaDataset).model
        val jenaResource = (resource as JenaNamedNode).node
        
        val referenceFormulationProp = ResourceFactory.createProperty(Rml.referenceFormulation)
        if (model.contains(jenaResource, referenceFormulationProp)) {
            val refFormulation = model.getProperty(jenaResource, referenceFormulationProp).`object`.asResource().uri
            return when (refFormulation) {
                "http://semweb.mmlab.be/ns/ql#CSV" -> setOf(CSVSource::class)
                "http://semweb.mmlab.be/ns/ql#JSONPath" -> setOf(JSONSourceRFC::class)
                "http://semweb.mmlab.be/ns/ql#XPath" -> setOf(XMLSource::class)
                else -> emptySet()
            }
        }
        
        // If it has rml:source and it's a D2RQ database or similar, return RDBSource
        val sourceProp = ResourceFactory.createProperty(Rml.source)
        if (model.contains(jenaResource, sourceProp)) {
            val sourceObj = model.getProperty(jenaResource, sourceProp).`object`
            if (sourceObj.isResource) {
                val types = model.listObjectsOfProperty(sourceObj.asResource(), org.apache.jena.vocabulary.RDF.type).toList()
                if (types.any { it.isResource && it.asResource().uri.startsWith("http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ") }) {
                    return setOf(RDBSource::class)
                }
            }
        }
        
        return emptySet()
    }
}
