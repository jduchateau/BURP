package rdfobjectloader

import rdfkt.toTerm as rdfktToTerm

typealias JenaNamedNode = rdfkt.JenaNamedNode
typealias JenaBlankNode = rdfkt.JenaBlankNode
typealias JenaLiteral = rdfkt.JenaLiteral
typealias JenaQuad = rdfkt.JenaQuad
typealias JenaDatasetCore = rdfkt.JenaDataset

fun org.apache.jena.rdf.model.RDFNode.toTerm(): rdf.Term = this.rdfktToTerm()
