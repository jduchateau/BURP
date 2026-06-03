@file:OptIn(ExperimentalJsExport::class)

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
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

fun parseTurtleFromStream(turtleReader: CharStream): ProvStore {
    val lexer = TurtleLexer(turtleReader)
    val tokenStream = CommonTokenStream(lexer)
    val parser = TurtleParser(tokenStream)

    // Parse the document
    val parseTree = parser.turtleDoc()

    // Visit with our annotating visitor
    val visitor = ProvTurtleVisitor()
    return visitor.visitTurtleDoc(parseTree)
}

@JsExport
fun parseTurtleFromString(turtleContent: String): ProvStore {
    val charStream = fromString(turtleContent)
    return parseTurtleFromStream(charStream)
}

@JsExport
fun parseTurtleFromFile(turtleFilePath: String): ProvStore {
    val source = SystemFileSystem.source(Path(turtleFilePath)).buffered()
    val charStream = fromString(source.readString(), turtleFilePath)
    return parseTurtleFromStream(charStream)
}