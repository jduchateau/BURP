package turtleprov

import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import java.io.File

fun parseTurtleFromFile(turtleFile: File): Dataset {
    val store = parseTurtleFromString(turtleFile.readText(Charsets.UTF_8))
    val model = store.toModel()
    return DatasetFactory.create(model)
}