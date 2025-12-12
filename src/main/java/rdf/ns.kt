package rdf

import rdf.Quad.Companion.asNamedTerm

interface Ontology {

    val prefix: String
    val base_uri: String

    operator fun invoke(iri: String) = NamedTerm("${base_uri}$iri")

}

object RDF : Ontology {

    override val prefix = "rdf"
    override val base_uri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

    val type = "${base_uri}type".asNamedTerm()
    val first = "${base_uri}first".asNamedTerm()
    val rest = "${base_uri}rest".asNamedTerm()
    val nil = "${base_uri}nil".asNamedTerm()
    val langString = "${base_uri}langString".asNamedTerm()
    val reifies = "${base_uri}reifies".asNamedTerm()
    val List = "${base_uri}List".asNamedTerm()
}

object XSD : Ontology {

    override val prefix = "xsd"
    override val base_uri = "http://www.w3.org/2001/XMLSchema#"


    val string = "${base_uri}string".asNamedTerm()
    val boolean = "${base_uri}boolean".asNamedTerm()
    val int = "${base_uri}int".asNamedTerm()
    val integer = "${base_uri}integer".asNamedTerm()
    val long = "${base_uri}long".asNamedTerm()
    val float = "${base_uri}float".asNamedTerm()
    val double = "${base_uri}double".asNamedTerm()
    val decimal = "${base_uri}decimal".asNamedTerm()
    val duration = "${base_uri}duration".asNamedTerm()
    val dateTime = "${base_uri}dateTime".asNamedTerm()
    val time = "${base_uri}time".asNamedTerm()
    val date = "${base_uri}date".asNamedTerm()
}


object RDEV : Ontology {

    override val prefix = "rdev"
    override val base_uri = "http://w3id.org/rmldevtools/"

    private fun property(local: String) = "${base_uri}$local".asNamedTerm()
    private fun resource(local: String) = "${base_uri}$local".asNamedTerm()

    val TOKEN = property("token")

    val FILE_ID = property("file")
    val START_LINE = property("startLine")
    val START_COLUMN = property("startColumn")
    val END_LINE = property("endLine")
    val END_COLUMN = property("endColumn")

    val STRING_START_LINE = property("startLine")
    val STRING_START_COLUMN = property("startColumn")
    val STRING_END_LINE = property("endLine")
    val STRING_END_COLUMN = property("endColumn")

    val BLANK_NODE_ID = property("blankNodeId")

    val SUBJECT = resource("SubjectProv")
    val PREDICATE = resource("PredicateProv")
    val OBJECT = resource("ObjectProv")

    // For testing manifest
    val TestTurtleAnnotation = resource("TestTurtleAnnotation")
}