package turtleprov

import org.apache.jena.query.Dataset
import java.io.File

fun parseTurtleFromFile(turtleFile: File): Dataset {
    val converter = JenaConverter()
    val store = parseTurtleFromString(turtleFile.readText(Charsets.UTF_8))
    return converter.run { store.toJenaDataset() }
}