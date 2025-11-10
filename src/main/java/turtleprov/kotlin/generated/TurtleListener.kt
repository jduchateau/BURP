// Generated from /home/jakub/Documents/Dev/RMLDevTools/provturtle/src/commonMain/grammar/Turtle.g4 by ANTLR 4.13.1
package be.uliege.RMLDevTools.parser.turtle.generated

import org.antlr.v4.kotlinruntime.tree.ParseTreeListener

/**
 * This interface defines a complete listener for a parse tree produced by [TurtleParser].
 */
public interface TurtleListener : ParseTreeListener {
    /**
     * Enter a parse tree produced by [TurtleParser.turtleDoc].
     *
     * @param ctx The parse tree
     */
    public fun enterTurtleDoc(ctx: TurtleParser.TurtleDocContext)

    /**
     * Exit a parse tree produced by [TurtleParser.turtleDoc].
     *
     * @param ctx The parse tree
     */
    public fun exitTurtleDoc(ctx: TurtleParser.TurtleDocContext)

    /**
     * Enter a parse tree produced by [TurtleParser.statement].
     *
     * @param ctx The parse tree
     */
    public fun enterStatement(ctx: TurtleParser.StatementContext)

    /**
     * Exit a parse tree produced by [TurtleParser.statement].
     *
     * @param ctx The parse tree
     */
    public fun exitStatement(ctx: TurtleParser.StatementContext)

    /**
     * Enter a parse tree produced by [TurtleParser.directive].
     *
     * @param ctx The parse tree
     */
    public fun enterDirective(ctx: TurtleParser.DirectiveContext)

    /**
     * Exit a parse tree produced by [TurtleParser.directive].
     *
     * @param ctx The parse tree
     */
    public fun exitDirective(ctx: TurtleParser.DirectiveContext)

    /**
     * Enter a parse tree produced by [TurtleParser.prefixID].
     *
     * @param ctx The parse tree
     */
    public fun enterPrefixID(ctx: TurtleParser.PrefixIDContext)

    /**
     * Exit a parse tree produced by [TurtleParser.prefixID].
     *
     * @param ctx The parse tree
     */
    public fun exitPrefixID(ctx: TurtleParser.PrefixIDContext)

    /**
     * Enter a parse tree produced by [TurtleParser.base].
     *
     * @param ctx The parse tree
     */
    public fun enterBase(ctx: TurtleParser.BaseContext)

    /**
     * Exit a parse tree produced by [TurtleParser.base].
     *
     * @param ctx The parse tree
     */
    public fun exitBase(ctx: TurtleParser.BaseContext)

    /**
     * Enter a parse tree produced by [TurtleParser.sparqlPrefix].
     *
     * @param ctx The parse tree
     */
    public fun enterSparqlPrefix(ctx: TurtleParser.SparqlPrefixContext)

    /**
     * Exit a parse tree produced by [TurtleParser.sparqlPrefix].
     *
     * @param ctx The parse tree
     */
    public fun exitSparqlPrefix(ctx: TurtleParser.SparqlPrefixContext)

    /**
     * Enter a parse tree produced by [TurtleParser.sparqlBase].
     *
     * @param ctx The parse tree
     */
    public fun enterSparqlBase(ctx: TurtleParser.SparqlBaseContext)

    /**
     * Exit a parse tree produced by [TurtleParser.sparqlBase].
     *
     * @param ctx The parse tree
     */
    public fun exitSparqlBase(ctx: TurtleParser.SparqlBaseContext)

    /**
     * Enter a parse tree produced by [TurtleParser.triples].
     *
     * @param ctx The parse tree
     */
    public fun enterTriples(ctx: TurtleParser.TriplesContext)

    /**
     * Exit a parse tree produced by [TurtleParser.triples].
     *
     * @param ctx The parse tree
     */
    public fun exitTriples(ctx: TurtleParser.TriplesContext)

    /**
     * Enter a parse tree produced by [TurtleParser.predicateObjectList].
     *
     * @param ctx The parse tree
     */
    public fun enterPredicateObjectList(ctx: TurtleParser.PredicateObjectListContext)

    /**
     * Exit a parse tree produced by [TurtleParser.predicateObjectList].
     *
     * @param ctx The parse tree
     */
    public fun exitPredicateObjectList(ctx: TurtleParser.PredicateObjectListContext)

    /**
     * Enter a parse tree produced by [TurtleParser.objectList].
     *
     * @param ctx The parse tree
     */
    public fun enterObjectList(ctx: TurtleParser.ObjectListContext)

    /**
     * Exit a parse tree produced by [TurtleParser.objectList].
     *
     * @param ctx The parse tree
     */
    public fun exitObjectList(ctx: TurtleParser.ObjectListContext)

    /**
     * Enter a parse tree produced by [TurtleParser.verb].
     *
     * @param ctx The parse tree
     */
    public fun enterVerb(ctx: TurtleParser.VerbContext)

    /**
     * Exit a parse tree produced by [TurtleParser.verb].
     *
     * @param ctx The parse tree
     */
    public fun exitVerb(ctx: TurtleParser.VerbContext)

    /**
     * Enter a parse tree produced by [TurtleParser.subject].
     *
     * @param ctx The parse tree
     */
    public fun enterSubject(ctx: TurtleParser.SubjectContext)

    /**
     * Exit a parse tree produced by [TurtleParser.subject].
     *
     * @param ctx The parse tree
     */
    public fun exitSubject(ctx: TurtleParser.SubjectContext)

    /**
     * Enter a parse tree produced by [TurtleParser.object_].
     *
     * @param ctx The parse tree
     */
    public fun enterObject_(ctx: TurtleParser.Object_Context)

    /**
     * Exit a parse tree produced by [TurtleParser.object_].
     *
     * @param ctx The parse tree
     */
    public fun exitObject_(ctx: TurtleParser.Object_Context)

    /**
     * Enter a parse tree produced by [TurtleParser.literal].
     *
     * @param ctx The parse tree
     */
    public fun enterLiteral(ctx: TurtleParser.LiteralContext)

    /**
     * Exit a parse tree produced by [TurtleParser.literal].
     *
     * @param ctx The parse tree
     */
    public fun exitLiteral(ctx: TurtleParser.LiteralContext)

    /**
     * Enter a parse tree produced by [TurtleParser.blankNodePropertyList].
     *
     * @param ctx The parse tree
     */
    public fun enterBlankNodePropertyList(ctx: TurtleParser.BlankNodePropertyListContext)

    /**
     * Exit a parse tree produced by [TurtleParser.blankNodePropertyList].
     *
     * @param ctx The parse tree
     */
    public fun exitBlankNodePropertyList(ctx: TurtleParser.BlankNodePropertyListContext)

    /**
     * Enter a parse tree produced by [TurtleParser.collection].
     *
     * @param ctx The parse tree
     */
    public fun enterCollection(ctx: TurtleParser.CollectionContext)

    /**
     * Exit a parse tree produced by [TurtleParser.collection].
     *
     * @param ctx The parse tree
     */
    public fun exitCollection(ctx: TurtleParser.CollectionContext)

    /**
     * Enter a parse tree produced by [TurtleParser.rdfLiteral].
     *
     * @param ctx The parse tree
     */
    public fun enterRdfLiteral(ctx: TurtleParser.RdfLiteralContext)

    /**
     * Exit a parse tree produced by [TurtleParser.rdfLiteral].
     *
     * @param ctx The parse tree
     */
    public fun exitRdfLiteral(ctx: TurtleParser.RdfLiteralContext)

    /**
     * Enter a parse tree produced by [TurtleParser.string].
     *
     * @param ctx The parse tree
     */
    public fun enterString(ctx: TurtleParser.StringContext)

    /**
     * Exit a parse tree produced by [TurtleParser.string].
     *
     * @param ctx The parse tree
     */
    public fun exitString(ctx: TurtleParser.StringContext)

    /**
     * Enter a parse tree produced by [TurtleParser.iri].
     *
     * @param ctx The parse tree
     */
    public fun enterIri(ctx: TurtleParser.IriContext)

    /**
     * Exit a parse tree produced by [TurtleParser.iri].
     *
     * @param ctx The parse tree
     */
    public fun exitIri(ctx: TurtleParser.IriContext)

    /**
     * Enter a parse tree produced by [TurtleParser.reifier].
     *
     * @param ctx The parse tree
     */
    public fun enterReifier(ctx: TurtleParser.ReifierContext)

    /**
     * Exit a parse tree produced by [TurtleParser.reifier].
     *
     * @param ctx The parse tree
     */
    public fun exitReifier(ctx: TurtleParser.ReifierContext)

    /**
     * Enter a parse tree produced by [TurtleParser.reifiedTriple].
     *
     * @param ctx The parse tree
     */
    public fun enterReifiedTriple(ctx: TurtleParser.ReifiedTripleContext)

    /**
     * Exit a parse tree produced by [TurtleParser.reifiedTriple].
     *
     * @param ctx The parse tree
     */
    public fun exitReifiedTriple(ctx: TurtleParser.ReifiedTripleContext)

    /**
     * Enter a parse tree produced by [TurtleParser.rtSubject].
     *
     * @param ctx The parse tree
     */
    public fun enterRtSubject(ctx: TurtleParser.RtSubjectContext)

    /**
     * Exit a parse tree produced by [TurtleParser.rtSubject].
     *
     * @param ctx The parse tree
     */
    public fun exitRtSubject(ctx: TurtleParser.RtSubjectContext)

    /**
     * Enter a parse tree produced by [TurtleParser.rtObject].
     *
     * @param ctx The parse tree
     */
    public fun enterRtObject(ctx: TurtleParser.RtObjectContext)

    /**
     * Exit a parse tree produced by [TurtleParser.rtObject].
     *
     * @param ctx The parse tree
     */
    public fun exitRtObject(ctx: TurtleParser.RtObjectContext)

    /**
     * Enter a parse tree produced by [TurtleParser.tripleTerm].
     *
     * @param ctx The parse tree
     */
    public fun enterTripleTerm(ctx: TurtleParser.TripleTermContext)

    /**
     * Exit a parse tree produced by [TurtleParser.tripleTerm].
     *
     * @param ctx The parse tree
     */
    public fun exitTripleTerm(ctx: TurtleParser.TripleTermContext)

    /**
     * Enter a parse tree produced by [TurtleParser.ttSubject].
     *
     * @param ctx The parse tree
     */
    public fun enterTtSubject(ctx: TurtleParser.TtSubjectContext)

    /**
     * Exit a parse tree produced by [TurtleParser.ttSubject].
     *
     * @param ctx The parse tree
     */
    public fun exitTtSubject(ctx: TurtleParser.TtSubjectContext)

    /**
     * Enter a parse tree produced by [TurtleParser.ttObject].
     *
     * @param ctx The parse tree
     */
    public fun enterTtObject(ctx: TurtleParser.TtObjectContext)

    /**
     * Exit a parse tree produced by [TurtleParser.ttObject].
     *
     * @param ctx The parse tree
     */
    public fun exitTtObject(ctx: TurtleParser.TtObjectContext)

    /**
     * Enter a parse tree produced by [TurtleParser.annotation].
     *
     * @param ctx The parse tree
     */
    public fun enterAnnotation(ctx: TurtleParser.AnnotationContext)

    /**
     * Exit a parse tree produced by [TurtleParser.annotation].
     *
     * @param ctx The parse tree
     */
    public fun exitAnnotation(ctx: TurtleParser.AnnotationContext)

    /**
     * Enter a parse tree produced by [TurtleParser.annotationBlock].
     *
     * @param ctx The parse tree
     */
    public fun enterAnnotationBlock(ctx: TurtleParser.AnnotationBlockContext)

    /**
     * Exit a parse tree produced by [TurtleParser.annotationBlock].
     *
     * @param ctx The parse tree
     */
    public fun exitAnnotationBlock(ctx: TurtleParser.AnnotationBlockContext)

}
