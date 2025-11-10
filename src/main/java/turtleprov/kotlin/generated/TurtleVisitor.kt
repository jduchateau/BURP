// Generated from /home/jakub/Documents/Dev/RMLDevTools/provturtle/src/commonMain/grammar/Turtle.g4 by ANTLR 4.13.1
package be.uliege.RMLDevTools.parser.turtle.generated

import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor

/**
 * This interface defines a complete generic visitor for a parse tree produced by [TurtleParser].
 *
 * @param T The return type of the visit operation.
 *   Use [Unit] for operations with no return type
 */
public interface TurtleVisitor<T> : ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by [TurtleParser.turtleDoc].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitTurtleDoc(ctx: TurtleParser.TurtleDocContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.statement].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitStatement(ctx: TurtleParser.StatementContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.directive].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitDirective(ctx: TurtleParser.DirectiveContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.prefixID].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitPrefixID(ctx: TurtleParser.PrefixIDContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.base].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitBase(ctx: TurtleParser.BaseContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.sparqlPrefix].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitSparqlPrefix(ctx: TurtleParser.SparqlPrefixContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.sparqlBase].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitSparqlBase(ctx: TurtleParser.SparqlBaseContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.triples].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitTriples(ctx: TurtleParser.TriplesContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.predicateObjectList].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitPredicateObjectList(ctx: TurtleParser.PredicateObjectListContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.objectList].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitObjectList(ctx: TurtleParser.ObjectListContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.verb].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitVerb(ctx: TurtleParser.VerbContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.subject].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitSubject(ctx: TurtleParser.SubjectContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.object_].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitObject_(ctx: TurtleParser.Object_Context): T

    /**
     * Visit a parse tree produced by [TurtleParser.literal].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitLiteral(ctx: TurtleParser.LiteralContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.blankNodePropertyList].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitBlankNodePropertyList(ctx: TurtleParser.BlankNodePropertyListContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.collection].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitCollection(ctx: TurtleParser.CollectionContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.rdfLiteral].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitRdfLiteral(ctx: TurtleParser.RdfLiteralContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.string].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitString(ctx: TurtleParser.StringContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.iri].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitIri(ctx: TurtleParser.IriContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.reifier].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitReifier(ctx: TurtleParser.ReifierContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.reifiedTriple].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitReifiedTriple(ctx: TurtleParser.ReifiedTripleContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.rtSubject].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitRtSubject(ctx: TurtleParser.RtSubjectContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.rtObject].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitRtObject(ctx: TurtleParser.RtObjectContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.tripleTerm].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitTripleTerm(ctx: TurtleParser.TripleTermContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.ttSubject].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitTtSubject(ctx: TurtleParser.TtSubjectContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.ttObject].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitTtObject(ctx: TurtleParser.TtObjectContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.annotation].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitAnnotation(ctx: TurtleParser.AnnotationContext): T

    /**
     * Visit a parse tree produced by [TurtleParser.annotationBlock].
     *
     * @param ctx The parse tree
     * @return The visitor result
     */
    public fun visitAnnotationBlock(ctx: TurtleParser.AnnotationBlockContext): T

}
