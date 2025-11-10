package turtleprov.kotlin

import be.uliege.RMLDevTools.parser.turtle.generated.TurtleLexer
import be.uliege.RMLDevTools.parser.turtle.generated.TurtleParser
import kotlinx.io.buffered
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams.fromString
import org.antlr.v4.kotlinruntime.CommonTokenStream
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString


fun parseTurtleFromString(turtleContent: String): ProvStore {
    val charStream: CharStream = fromString(turtleContent)
    val lexer = TurtleLexer(charStream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = TurtleParser(tokenStream)

    // Parse the document
    val parseTree = parser.turtleDoc()

    // Visit with our annotating visitor
    val visitor = ProvTurtleVisitor()
    return visitor.visitTurtleDoc(parseTree)
}


fun parseTurtleFromFile(turtleFile: Path): ProvStore {
    val source = SystemFileSystem.source(turtleFile).buffered()
    val content = source.readString()
    return parseTurtleFromString(content)
}