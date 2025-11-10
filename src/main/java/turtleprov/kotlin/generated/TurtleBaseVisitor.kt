// Generated from /home/jakub/Documents/Dev/RMLDevTools/provturtle/src/commonMain/grammar/Turtle.g4 by ANTLR 4.13.1
package be.uliege.RMLDevTools.parser.turtle.generated

import org.antlr.v4.kotlinruntime.tree.AbstractParseTreeVisitor

/**
 * This class provides an empty implementation of [TurtleVisitor],
 * which can be extended to create a visitor which only needs to handle a subset
 * of the available methods.
 *
 * @param T The return type of the visit operation.
 *   Use [Unit] for operations with no return type
 */
public abstract class TurtleBaseVisitor<T> : AbstractParseTreeVisitor<T>(), TurtleVisitor<T> {
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitTurtleDoc(ctx: TurtleParser.TurtleDocContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitStatement(ctx: TurtleParser.StatementContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitDirective(ctx: TurtleParser.DirectiveContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitPrefixID(ctx: TurtleParser.PrefixIDContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitBase(ctx: TurtleParser.BaseContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitSparqlPrefix(ctx: TurtleParser.SparqlPrefixContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitSparqlBase(ctx: TurtleParser.SparqlBaseContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitTriples(ctx: TurtleParser.TriplesContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitPredicateObjectList(ctx: TurtleParser.PredicateObjectListContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitObjectList(ctx: TurtleParser.ObjectListContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitVerb(ctx: TurtleParser.VerbContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitSubject(ctx: TurtleParser.SubjectContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitObject_(ctx: TurtleParser.Object_Context): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitLiteral(ctx: TurtleParser.LiteralContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitBlankNodePropertyList(ctx: TurtleParser.BlankNodePropertyListContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitCollection(ctx: TurtleParser.CollectionContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitRdfLiteral(ctx: TurtleParser.RdfLiteralContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitString(ctx: TurtleParser.StringContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitIri(ctx: TurtleParser.IriContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitReifier(ctx: TurtleParser.ReifierContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitReifiedTriple(ctx: TurtleParser.ReifiedTripleContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitRtSubject(ctx: TurtleParser.RtSubjectContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitRtObject(ctx: TurtleParser.RtObjectContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitTripleTerm(ctx: TurtleParser.TripleTermContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitTtSubject(ctx: TurtleParser.TtSubjectContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitTtObject(ctx: TurtleParser.TtObjectContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitAnnotation(ctx: TurtleParser.AnnotationContext): T {
        return this.visitChildren(ctx)
    }
    /**
     * The default implementation returns the result of calling [visitChildren] on [ctx].
     */
    override fun visitAnnotationBlock(ctx: TurtleParser.AnnotationBlockContext): T {
        return this.visitChildren(ctx)
    }
}
