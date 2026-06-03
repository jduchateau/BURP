package turtleprov

import org.antlr.v4.kotlinruntime.tree.TerminalNode
import rdfkt.*
import rdfkt.Quad.Companion.asLiteralTerm
import rdfobjectloader.Point
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
    private val blankNodeMap: MutableMap<String, BlankTerm> = mutableMapOf()

    private var blankNodeCounter = 0
    private fun createBlankNode(): BlankTerm = BlankTerm.from(blankNodeCounter++)

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

            ctx.reifiedTriple() != null -> {
                val subject = visitReifiedTriple(ctx.reifiedTriple()!!)
                predicateObjectListCtx?.let { pol ->
                    visitPredicateObjectList(pol, subject)
                }
            }
        }
        return null
    }

    override fun visitSubject(ctx: TurtleParser.SubjectContext): Pair<BlankNodeOrIRI, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.collection() != null -> visitCollection(ctx.collection()!!)
            else -> throw IllegalArgumentException("Unknown subject type")
        }
    }

    override fun visitIri(ctx: TurtleParser.IriContext): Pair<NamedTerm, NodeInfo> {
        val irirefCtx = ctx.IRIREF()
        val prefixedNameCtx = ctx.PrefixedName()
        return when {
            irirefCtx != null -> {
                val token = irirefCtx.symbol
                val iri = token.text!!.removeSurrounding("<", ">")
                val resource = NamedTerm(iri)
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
                    val resource = NamedTerm(namespace + localName)
                    val nodeInfo = NodeInfo(TurtleNodeKind.PREFIXED_NAME, token.startPoint(), token.endPoint())
                    Pair(resource, nodeInfo)
                } else {
                    throw IllegalArgumentException("Invalid prefixed name: $prefixedName")
                }
            }

            else -> throw IllegalArgumentException("Unknown IRI type")
        }
    }

    private fun visitBlankNodeTerminal(terminalNode: TerminalNode): Pair<BlankTerm, NodeInfo> {
        val token = terminalNode.symbol
        val text = token.text ?: "" //fixme is it intended to be empty string

        return when {
            text.startsWith("_:") -> {
                val matcher = blankNodeLabelPattern.matchEntire(text)
                if (matcher != null) {
                    val label = matcher.groups[1]!!.value
                    val hash = label.hashCode()
                    val resource = blankNodeMap.getOrPut(label) { createBlankNode() }
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


    fun assembleList(list: List<Term>): BlankNodeOrIRI =
        when {
            list.isEmpty() -> RDF.nil
            else -> {
                val blankNodes = List(list.size) { createBlankNode() }
                blankNodes.zip(list).forEach { (node, value) ->
                    store.quads.add(ProvQuad(Quad(node, RDF.first, value)))
                    store.quads.add(ProvQuad(Quad(node, RDF.type, RDF.List)))
                }
                blankNodes.zipWithNext().forEach { (current, next) ->
                    store.quads.add(ProvQuad(Quad(current, RDF.rest, next)))
                }
                store.quads.add(ProvQuad(Quad(blankNodes.last(), RDF.rest, RDF.nil)))
                blankNodes.first()
            }
        }

    override fun visitCollection(ctx: TurtleParser.CollectionContext): Pair<BlankNodeOrIRI, NodeInfo> {
        val items = ctx.object_().map { visitObject_(it) }.map { it.first }
        val collectionStart = assembleList(items)
        val nodeInfo = NodeInfo(
            TurtleNodeKind.COLLECTION,
            ctx.start?.startPoint(), ctx.stop?.endPoint()
        )

        return Pair(collectionStart, nodeInfo)
    }

    override fun visitBlankNodePropertyList(ctx: TurtleParser.BlankNodePropertyListContext): Pair<BlankNodeOrIRI, NodeInfo> {
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
        subject: Pair<BlankNodeOrIRI, NodeInfo>
    ) {
        val verbs = ctx.verb()
        val objectLists = ctx.objectList()

        for (i in verbs.indices) {
            val predicate = visitVerb(verbs[i])
            val objectListCtx = objectLists[i]
            val objects = objectListCtx.object_()
            val annotations = objectListCtx.annotation()

            for (j in objects.indices) {
                val objCtx = objects[j]
                val obj = visitObject_(objCtx)
                
                val stmt = Quad(subject.first, predicate.first, obj.first)
                store.quads.add(ProvQuad(stmt, subject.second, predicate.second, obj.second))
                
                val annotationCtx = annotations.getOrNull(j)
                if (annotationCtx != null) {
                    processAnnotation(annotationCtx, stmt, subject.second, predicate.second, obj.second)
                }
            }
        }
    }

    private fun processAnnotation(
        ctx: TurtleParser.AnnotationContext,
        mainQuad: Quad,
        subjInfo: NodeInfo?,
        predInfo: NodeInfo?,
        objInfo: NodeInfo?
    ) {
        val reifiers = ctx.reifier()
        val annotationBlocks = ctx.annotationBlock()
        
        if (reifiers.isEmpty() && annotationBlocks.isEmpty()) return
        
        var reifier: BlankNodeOrIRI? = null
        for (reifierCtx in reifiers) {
            if (reifierCtx.iri() != null) {
                reifier = visitIri(reifierCtx.iri()!!).first
                break
            } else if (reifierCtx.BlankNode() != null) {
                reifier = visitBlankNodeTerminal(reifierCtx.BlankNode()!!).first
                break
            }
        }
        
        if (reifier == null) {
            reifier = createBlankNode()
        }
        
        val reificationQuad = Quad(reifier, RDF.reifies, mainQuad)
        store.quads.add(ProvQuad(reificationQuad))
        
        for (blockCtx in annotationBlocks) {
            val pol = blockCtx.predicateObjectList()
            val reifierPair = Pair(reifier, NodeInfo(TurtleNodeKind.ANONYMOUS_BLANK_NODE, blockCtx.start?.startPoint(), blockCtx.stop?.endPoint()))
            visitPredicateObjectList(pol, reifierPair)
        }
    }

    override fun visitVerb(ctx: TurtleParser.VerbContext): Pair<NamedTerm, NodeInfo> {
        return when {
            ctx.iri() != null -> {
                val (property, nodeInfo) = visitIri(ctx.iri()!!)
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

    override fun visitObjectList(ctx: TurtleParser.ObjectListContext): List<Pair<Term, NodeInfo>> {
        // For now, ignore annotations and just return objects
        return ctx.object_().map { visitObject_(it) }
    }

    override fun visitObject_(ctx: TurtleParser.Object_Context): Pair<Term, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.collection() != null -> visitCollection(ctx.collection()!!)
            ctx.blankNodePropertyList() != null -> visitBlankNodePropertyList(ctx.blankNodePropertyList()!!)
            ctx.literal() != null -> visitLiteral(ctx.literal()!!)
            ctx.tripleTerm() != null -> visitTripleTerm(ctx.tripleTerm()!!)
            ctx.reifiedTriple() != null -> visitReifiedTriple(ctx.reifiedTriple()!!)
            else -> throw IllegalArgumentException("Unknown object type")
        }
    }

    override fun visitLiteral(ctx: TurtleParser.LiteralContext): Pair<Term, NodeInfo> {
        return when {
            ctx.rdfLiteral() != null -> visitRdfLiteral(ctx.rdfLiteral()!!)
            ctx.NumericLiteral() != null -> visitNumericLiteral(ctx.NumericLiteral()!!)
            ctx.BooleanLiteral() != null -> visitBooleanLiteral(ctx.BooleanLiteral()!!)
            else -> throw IllegalArgumentException("Unknown literal type")
        }
    }

    override fun visitRdfLiteral(ctx: TurtleParser.RdfLiteralContext): Pair<Term, NodeInfo> {
        val (stringValue, quoteSize) = visitString(ctx.string())

        val langDirCtx = ctx.LANG_DIR()
        val iriCtx = ctx.iri()
        val literal = when {
            langDirCtx != null -> {
                val langTag = langDirCtx.text.substring(1) // Remove @
                Literal(stringValue, type = XSD.string, lang = langTag)
            }

            iriCtx != null -> {
                val datatype = (visitIri(iriCtx)).first
                Literal(stringValue, datatype)
            }

            else -> Literal(stringValue, type = XSD.string) // RDF 1.2 specifies default to be string
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

    private fun visitNumericLiteral(terminalNode: TerminalNode): Pair<Term, NodeInfo> {
        val token = terminalNode.symbol
        val text = token.text ?: ""

        val (literal, kind) = when {
            text.contains('.') && (text.contains('e') || text.contains('E')) -> {
                text.toDouble().asLiteralTerm() to TurtleNodeKind.DOUBLE_LITERAL
            }

            text.contains('.') -> {
                text.toDouble().asLiteralTerm() to TurtleNodeKind.DECIMAL_LITERAL // TODO add decimal
            }

            else -> {
                text.toInt().asLiteralTerm() to TurtleNodeKind.INTEGER_LITERAL
            }
        }

        return Pair(literal, NodeInfo(kind, token.startPoint(), token.endPoint()))
    }

    private fun visitBooleanLiteral(terminalNode: TerminalNode): Pair<Term, NodeInfo> {
        val token = terminalNode.symbol
        val text = token.text
        val literal = text.toBoolean().asLiteralTerm()
        val nodeInfo = NodeInfo(TurtleNodeKind.BOOLEAN_LITERAL, token.startPoint(), token.endPoint())
        return Pair(literal, nodeInfo)
    }

    override fun visitReifiedTriple(ctx: TurtleParser.ReifiedTripleContext): Pair<BlankNodeOrIRI, NodeInfo> {
        val s = visitRtSubject(ctx.rtSubject())
        val p = visitVerb(ctx.verb())
        val o = visitRtObject(ctx.rtObject())
        val triple = Quad(s.first, p.first, o.first)
        
        val reifierCtx = ctx.reifier()
        val reifier: BlankNodeOrIRI = when {
            reifierCtx == null -> createBlankNode()
            reifierCtx.iri() != null -> visitIri(reifierCtx.iri()!!).first
            reifierCtx.BlankNode() != null -> visitBlankNodeTerminal(reifierCtx.BlankNode()!!).first
            else -> createBlankNode()
        }
        
        val reificationQuad = Quad(reifier, RDF.reifies, triple)
        store.quads.add(ProvQuad(reificationQuad))
        
        val nodeInfo = NodeInfo(
            TurtleNodeKind.REIFIED_TRIPLE,
            ctx.start?.startPoint().toMyPoint(),
            ctx.stop?.endPoint().toMyPoint()
        )
        return Pair(reifier, nodeInfo)
    }

    override fun visitTripleTerm(ctx: TurtleParser.TripleTermContext): Pair<Term, NodeInfo> {
        val s = visitTtSubject(ctx.ttSubject())
        val p = visitVerb(ctx.verb())
        val o = visitTtObject(ctx.ttObject())
        val triple = Quad(s.first, p.first, o.first)
        
        val nodeInfo = NodeInfo(
            TurtleNodeKind.TRIPLE_TERM,
            ctx.start?.startPoint().toMyPoint(),
            ctx.stop?.endPoint().toMyPoint()
        )
        return Pair(triple, nodeInfo)
    }

    override fun visitRtSubject(ctx: TurtleParser.RtSubjectContext): Pair<BlankNodeOrIRI, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.reifiedTriple() != null -> visitReifiedTriple(ctx.reifiedTriple()!!)
            else -> throw IllegalArgumentException("Unknown rtSubject type")
        }
    }

    override fun visitRtObject(ctx: TurtleParser.RtObjectContext): Pair<Term, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.literal() != null -> visitLiteral(ctx.literal()!!)
            ctx.tripleTerm() != null -> visitTripleTerm(ctx.tripleTerm()!!)
            ctx.reifiedTriple() != null -> visitReifiedTriple(ctx.reifiedTriple()!!)
            else -> throw IllegalArgumentException("Unknown rtObject type")
        }
    }

    override fun visitTtSubject(ctx: TurtleParser.TtSubjectContext): Pair<BlankNodeOrIRI, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            else -> throw IllegalArgumentException("Unknown ttSubject type")
        }
    }

    override fun visitTtObject(ctx: TurtleParser.TtObjectContext): Pair<Term, NodeInfo> {
        return when {
            ctx.iri() != null -> visitIri(ctx.iri()!!)
            ctx.BlankNode() != null -> visitBlankNodeTerminal(ctx.BlankNode()!!)
            ctx.literal() != null -> visitLiteral(ctx.literal()!!)
            ctx.tripleTerm() != null -> visitTripleTerm(ctx.tripleTerm()!!)
            else -> throw IllegalArgumentException("Unknown ttObject type")
        }
    }
}


