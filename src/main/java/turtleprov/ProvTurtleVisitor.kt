package turtleprov

import org.antlr.v4.kotlinruntime.tree.TerminalNode
import org.apache.jena.rdf.model.AnonId
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFList
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF
import turtleprov.generated.TurtleBaseVisitor
import turtleprov.generated.TurtleParser
import turtleprov.generated.TurtleParser.Tokens

import org.antlr.v4.kotlinruntime.ast.Point as AntlrPoint

private fun AntlrPoint?.toMyPoint(): Point? {
    return Point(this?.line?.minus(1) ?: return null, this.column)
}

/**
 * Visitor that converts Turtle parse tree to Jena Dataset with RDF 1.2 annotations
 *
 * TODO Improve Error reporting and add the error listener.
 */
class ProvTurtleVisitor : TurtleBaseVisitor<Any?>() {
    private val store = ProvStore()
    private val model = ModelFactory.createDefaultModel()
    private val blankNodeMap: MutableMap<String, Resource> = mutableMapOf()
    private fun createBlankNode(id: String? = null): Resource =
        model.createResource(if (id != null) AnonId(id) else AnonId())

    // Patterns for parsing
    private val iriRefPattern = Regex("^<(.*)>$")
    private val prefixedNamePattern = Regex("^(.*?):(.*)$")

    private val blankNodeLabelPattern = Regex("^_:(.+)$")

    override fun defaultResult(): Any? = null

    override fun visitTurtleDoc(ctx: TurtleParser.TurtleDocContext): ProvStore {
        ctx.statement().forEach { visit(it) }
        return store
    }

    override fun visitStatement(ctx: TurtleParser.StatementContext): Any? {
        val directive = ctx.directive()
        if (directive != null) {
            return visitDirective(directive)
        }
        val triples = ctx.triples()
        if (triples != null) {
            return visit(triples)
        }
        return null
    }

    fun makeRegisterPrefix(prefix: TerminalNode, iriref: TerminalNode, isSparql: Boolean): Any? {
        val prefixNs = prefix.text.removeSuffix(":")
        val iri = iriref.text.removeSurrounding("<", ">")
        store.prefixes[prefixNs] = iri
        return null
    }

    override fun visitPrefixID(ctx: TurtleParser.PrefixIDContext): Any? {
        return makeRegisterPrefix(ctx.PNAME_NS(), ctx.IRIREF(), false)
    }

    override fun visitSparqlPrefix(ctx: TurtleParser.SparqlPrefixContext): Any? {
        return makeRegisterPrefix(ctx.PNAME_NS(), ctx.IRIREF(), false)
    }

    override fun visitBase(ctx: TurtleParser.BaseContext): Any? {
        // Handle base IRI if needed
        return null
    }

    override fun visitSparqlBase(ctx: TurtleParser.SparqlBaseContext): Any? {
        // Handle SPARQL base IRI if needed
        return null
    }

    override fun visitTriples(ctx: TurtleParser.TriplesContext): Any? {
        val subjectCtx = ctx.subject()
        val predicateObjectListCtx = ctx.predicateObjectList()
        val blankNodePropertyListCtx = ctx.blankNodePropertyList()
        when {
            subjectCtx != null && predicateObjectListCtx != null -> {
                val subject = visitSubject(subjectCtx)
                visitPredicateObjectList(predicateObjectListCtx, subject)
            }

            blankNodePropertyListCtx != null -> {
                val blankNode = visitBlankNodePropertyList(blankNodePropertyListCtx)
                predicateObjectListCtx?.let { pol ->
                    visitPredicateObjectList(pol, blankNode)
                }
            }
            // TODO: Handle reifiedTriple case
        }
        return null
    }

    override fun visitSubject(ctx: TurtleParser.SubjectContext): Pair<Resource, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.collection() != null -> visitCollection(ctx.collection()!!)
            else -> throw IllegalArgumentException("Unknown subject type")
        }
    }

    override fun visitIri(ctx: TurtleParser.IriContext): Pair<Resource, NodeInfo> {
        val irirefCtx = ctx.IRIREF()
        val prefixedNameCtx = ctx.PrefixedName()
        return when {
            irirefCtx != null -> {
                val token = irirefCtx.symbol
                val iri = token.text!!.removeSurrounding("<", ">")
                val resource = ResourceFactory.createResource(iri)
                val nodeInfo = NodeInfo(
                    TurtleNodeKind.IRIREF,
                    token.startPoint(),
                    token.endPoint()
                )
                Pair(resource, nodeInfo)
            }

            prefixedNameCtx != null -> {
                val token = prefixedNameCtx.symbol
                val prefixedName = token.text ?: ""
                val match = prefixedNamePattern.matchEntire(prefixedName)

                if (match != null) {
                    val prefix = match.groups[1]?.value ?: ""
                    val localName = match.groups[2]?.value
                    val namespace = store.prefixes[prefix] ?: throw IllegalArgumentException("Unknown prefix: $prefix")
                    val resource = ResourceFactory.createResource(namespace + localName)
                    val nodeInfo = NodeInfo(TurtleNodeKind.PREFIXED_NAME, token.startPoint(), token.endPoint())
                    Pair(resource, nodeInfo)
                } else {
                    throw IllegalArgumentException("Invalid prefixed name: $prefixedName")
                }
            }

            else -> throw IllegalArgumentException("Unknown IRI type")
        }
    }

    private fun visitBlankNodeTerminal(terminalNode: TerminalNode): Pair<Resource, NodeInfo> {
        val token = terminalNode.symbol
        val text = token.text ?: "" //fixme is it intended to be empty string

        return when {
            text.startsWith("_:") -> {
                val matcher = blankNodeLabelPattern.matchEntire(text)
                if (matcher != null) {
                    val label = matcher.groups[1]!!.value
                    val resource = blankNodeMap.getOrPut(label) { createBlankNode(label) }
                    val nodeInfo = NodeInfo(
                        TurtleNodeKind.BLANK_NODE_LABEL,
                        token.startPoint(), token.endPoint(),
                        label
                    )
                    Pair(resource, nodeInfo)
                } else {
                    throw IllegalArgumentException("Invalid blank node label: $text")
                }
            }

            text.startsWith('[') && text.endsWith(']') -> {
                val resource = createBlankNode()
                val nodeInfo = NodeInfo(
                    TurtleNodeKind.ANONYMOUS_BLANK_NODE,
                    token.startPoint(), token.endPoint()
                )
                Pair(resource, nodeInfo)
            }

            else -> throw IllegalArgumentException("Unknown blank node format: $text")
        }
    }


    fun assembleList(list: List<RDFNode>): Resource =
        when {
            list.isEmpty() -> RDF.nil
            else -> {
                val blankNodes = List(list.size) { createBlankNode() }
                blankNodes.zip(list).forEach { (node, value) ->
                    store.triples.add(ProvTriple(model.createStatement(node, RDF.first, value)))
                    store.triples.add(ProvTriple(model.createStatement(node, RDF.type, RDF.List)))
                }
                blankNodes.zipWithNext().forEach { (current, next) ->
                    store.triples.add(ProvTriple(model.createStatement(current, RDF.rest, next)))
                }
                store.triples.add(ProvTriple(model.createStatement(blankNodes.last(), RDF.rest, RDF.nil)))
                blankNodes.first()
            }
        }

    override fun visitCollection(ctx: TurtleParser.CollectionContext): Pair<Resource, NodeInfo> {
        val items = ctx.object_().map { visitObject_(it) }.map { it.first }
        val collectionStart = assembleList(items)
        val nodeInfo = NodeInfo(
            TurtleNodeKind.COLLECTION,
            ctx.start?.startPoint(), ctx.stop?.endPoint()
        )

        return Pair(collectionStart, nodeInfo)
    }

    override fun visitBlankNodePropertyList(ctx: TurtleParser.BlankNodePropertyListContext): Pair<Resource, NodeInfo> {
        val resource = createBlankNode()
        val nodeInfo = NodeInfo(
            TurtleNodeKind.BLANK_NODE_PROPERTY_LIST,
            ctx.start?.startPoint(), ctx.stop?.endPoint()
        )

        val subject = Pair(resource, nodeInfo)
        visitPredicateObjectList(ctx.predicateObjectList(), subject)

        return subject
    }

    private fun visitPredicateObjectList(
        ctx: TurtleParser.PredicateObjectListContext,
        subject: Pair<Resource, NodeInfo>
    ) {
        val verbs = ctx.verb()
        val objectLists = ctx.objectList()

        for (i in verbs.indices) {
            val predicate = visitVerb(verbs[i])
            val objects = visitObjectList(objectLists[i])

            objects.forEach { obj ->
                val stmt = model.createStatement(subject.first, predicate.first, obj.first)
                store.triples.add(ProvTriple(stmt, subject.second, predicate.second, obj.second))
            }
        }
    }

    override fun visitVerb(ctx: TurtleParser.VerbContext): Pair<Property, NodeInfo> {
        return when {
            ctx.iri() != null -> {
                val (resource, nodeInfo) = visitIri(ctx.iri()!!)
                val property = model.createProperty(resource.uri)
                Pair(property, nodeInfo)
            }

            ctx.text == "a" -> {
                val nodeInfo = NodeInfo(
                    TurtleNodeKind.TYPE_VERB,
                    ctx.start?.startPoint(), ctx.stop?.endPoint()
                )
                Pair(RDF.type, nodeInfo)
            }

            else -> throw IllegalArgumentException("Unknown verb type: ${ctx.text}")
        }
    }

    override fun visitObjectList(ctx: TurtleParser.ObjectListContext): List<Pair<RDFNode, NodeInfo>> {
        // For now, ignore annotations and just return objects
        return ctx.object_().map { visitObject_(it) }
    }

    override fun visitObject_(ctx: TurtleParser.Object_Context): Pair<RDFNode, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.collection() != null -> visitCollection(ctx.collection()!!)
            ctx.blankNodePropertyList() != null -> visitBlankNodePropertyList(ctx.blankNodePropertyList()!!)
            ctx.literal() != null -> visitLiteral(ctx.literal()!!)
            else -> throw IllegalArgumentException("Unknown object type")
        }
    }

    override fun visitLiteral(ctx: TurtleParser.LiteralContext): Pair<Literal, NodeInfo> {
        return when {
            ctx.rdfLiteral() != null -> visitRdfLiteral(ctx.rdfLiteral()!!)
            ctx.NumericLiteral() != null -> visitNumericLiteral(ctx.NumericLiteral()!!)
            ctx.BooleanLiteral() != null -> visitBooleanLiteral(ctx.BooleanLiteral()!!)
            else -> throw IllegalArgumentException("Unknown literal type")
        }
    }

    override fun visitRdfLiteral(ctx: TurtleParser.RdfLiteralContext): Pair<Literal, NodeInfo> {
        val (stringValue, quoteSize) = visitString(ctx.string())

        val langDirCtx = ctx.LANG_DIR()
        val iriCtx = ctx.iri()
        val literal = when {
            langDirCtx != null -> {
                val langTag = langDirCtx.text.substring(1) // Remove @
                model.createLiteral(stringValue, langTag)
            }

            iriCtx != null -> {
                val datatype = (visitIri(iriCtx)).first
                model.createTypedLiteral(stringValue, datatype.uri)
            }

            else -> model.createTypedLiteral(stringValue) // RDF 1.2 specifies default to be string
        }

        val kind = when ((ctx.string().children?.first() as TerminalNode).symbol.type) {
            Tokens.STRING_LITERAL_QUOTE -> TurtleNodeKind.STRING_LITERAL_QUOTE
            Tokens.STRING_LITERAL_SINGLE_QUOTE -> TurtleNodeKind.STRING_LITERAL_SINGLE_QUOTE
            Tokens.STRING_LITERAL_LONG_QUOTE -> TurtleNodeKind.STRING_LITERAL_LONG_QUOTE
            Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE -> TurtleNodeKind.STRING_LITERAL_LONG_SINGLE_QUOTE
            else -> TurtleNodeKind.STRING_LITERAL_QUOTE
        }

        val nodeInfo = NodeInfo(
            kind,
            ctx.start?.startPoint().toMyPoint(),
            ctx.stop?.endPoint().toMyPoint(),
            rdfLiteralStringStart = ctx.string().start?.startPoint().toMyPoint()?.plus(Point(0, quoteSize)),
            rdfLiteralStringEnd = ctx.string().stop?.endPoint().toMyPoint()?.minus(Point(0, quoteSize)),
        )
        return Pair(literal, nodeInfo)
    }

    override fun visitString(ctx: TurtleParser.StringContext): Pair<String, Int> {
        val text = ctx.text
        return when {
            text.startsWith("\"\"\"") && text.endsWith("\"\"\"") -> text.substring(3, text.length - 3) to 3
            text.startsWith("'''") && text.endsWith("'''") -> text.substring(3, text.length - 3) to 3
            text.startsWith("\"") && text.endsWith("\"") -> text.substring(1, text.length - 1) to 1
            text.startsWith("'") && text.endsWith("'") -> text.substring(1, text.length - 1) to 1
            else -> text to 0
        }
    }

    private fun visitNumericLiteral(terminalNode: TerminalNode): Pair<Literal, NodeInfo> {
        val token = terminalNode.symbol
        val text = token.text ?: ""

        val (literal, kind) = when {
            text.contains('.') && (text.contains('e') || text.contains('E')) -> {
                model.createTypedLiteral(text.toDouble()) to TurtleNodeKind.DOUBLE_LITERAL
            }

            text.contains('.') -> {
                model.createTypedLiteral(text.toDouble()) to TurtleNodeKind.DECIMAL_LITERAL // TODO add decimal
            }

            else -> {
                model.createTypedLiteral(text.toInt()) to TurtleNodeKind.INTEGER_LITERAL
            }
        }

        return Pair(literal, NodeInfo(kind, token.startPoint(), token.endPoint()))
    }

    private fun visitBooleanLiteral(terminalNode: TerminalNode): Pair<Literal, NodeInfo> {
        val token = terminalNode.symbol
        val text = token.text
        val literal = model.createTypedLiteral(text.toBoolean())
        val nodeInfo = NodeInfo(TurtleNodeKind.BOOLEAN_LITERAL, token.startPoint(), token.endPoint())
        return Pair(literal, nodeInfo)
    }
}


