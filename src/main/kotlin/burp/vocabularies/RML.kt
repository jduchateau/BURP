package burp.vocabularies

import org.apache.jena.rdf.model.ResourceFactory

object RML {
    const val NS = "http://w3id.org/rml/"

    private fun property(local: String) = ResourceFactory.createProperty("${NS}$local")
    private fun resource(local: String) = ResourceFactory.createResource("${NS}$local")

    val BLANKNODE = resource("BlankNode")
    val IRI = resource("IRI")
    val URI = resource("URI")
    val UnsafeIRI = resource("UnsafeIRI")
    val LITERAL = resource("Literal")

    // RML shortcuts
    val subject = property("subject")
    val `object` = property("object")
    val predicate = property("predicate")
    val graph = property("graph")
    val language = property("language")
    val datatype = property("datatype")
    val child = property("child")
    val parent = property("parent")
    val return_ = property("return")
    val function = property("function")
    val parameter = property("parameter")
    val inputValue = property("inputValue")

    // RML Constants
    val defaultGraph = resource("defaultGraph")
    val CSV = resource("CSV")
    val JSONPath = resource("JSONPath")
    val MappingDirectory = resource("MappingDirectory")
    val CurrentWorkingDirectory = resource("CurrentWorkingDirectory")
    val Namespace = resource("Namespace")
    val RelativePathSource = resource("RelativePathSource")
    val FilePath = resource("FilePath")
    val SQL2008Table = resource("SQL2008Table")
    val SQL2008Query = resource("SQL2008Query")
    val XPath = resource("XPath")
    val XPathReferenceFormulation = resource("XPathReferenceFormulation")

    val append = resource("append")
    val cartesianProduct = resource("cartesianProduct")

    val UTF8 = resource("UTF-8")
    val UTF16 = resource("UTF-16")
    val none = resource("none")
    val gzip = resource("gzip")
    val zip = resource("zip")
    val tarxz = resource("tarxz")
    val targz = resource("targz")

    const val FORMATSNS = "http://www.w3.org/ns/formats/"
    private fun formatsResource(local: String) = ResourceFactory.createResource("${FORMATSNS}$local")
    val SPARQL_Results_CSV = formatsResource("SPARQL_Results_CSV")
    val SPARQL_Results_TSV = formatsResource("SPARQL_Results_TSV")
    val SPARQL_Results_JSON = formatsResource("SPARQL_Results_JSON")
    val SPARQL_Results_XML = formatsResource("SPARQL_Results_XML")

    // Classes
    // We currently have no need for classes and that limits the memory footprint

    // Properties
    val allowEmptyListAndContainer = property("allowEmptyListAndContainer")
    val baseIRI = property("baseIRI")
    val clazz = property("class")
    val childMap = property("childMap")
    val compression = property("compression")
    val constant = property("constant")
    val datatypeMap = property("datatypeMap")
    val encoding = property("encoding")
    val functionExecution = property("functionExecution")
    val functionMap = property("functionMap")
    val field = property("field")
    val fieldName = property("fieldName")
    val gather = property("gather")
    val gatherAs = property("gatherAs")
    val graphMap = property("graphMap")
    val innerJoin = property("innerJoin")
    val input = property("input")
    val inputValueMap = property("inputValueMap")
    val iterator = property("iterator")
    val joinCondition = property("joinCondition")
    val leftJoin = property("leftJoin")
    val languageMap = property("languageMap")
    val logicalSource = property("logicalSource")
    val namespace = property("namespace")
    val namespacePrefix = property("namespacePrefix")
    val namespaceURL = property("namespaceURL")
    val NULL = property("null")
    val objectMap = property("objectMap")
    val parameterMap = property("parameterMap")
    val parentLogicalView = property("parentLogicalView")
    val path = property("path")
    val parentMap = property("parentMap")
    val parentTriplesMap = property("parentTriplesMap")
    val predicateMap = property("predicateMap")
    val predicateObjectMap = property("predicateObjectMap")
    val reference = property("reference")
    val referenceFormulation = property("referenceFormulation")
    val returnMap = property("returnMap")
    val root = property("root")
    val source = property("source")
    val strategy = property("strategy")
    val subjectMap = property("subjectMap")
    val template = property("template")
    val termType = property("termType")
    val viewOn = property("viewOn")

    // RML-IO targets
    val logicalTarget = property("logicalTarget")
    val target = property("target")
    val serialization = property("serialization")
}
