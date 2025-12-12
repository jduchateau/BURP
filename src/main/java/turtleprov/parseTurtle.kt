package turtleprov

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams.fromString
import org.antlr.v4.kotlinruntime.CommonTokenStream
import turtleprov.generated.TurtleLexer
import turtleprov.generated.TurtleParser


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