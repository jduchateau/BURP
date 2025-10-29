// Amper module: dependencies declared in scripts/module.yaml

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


private val ROOT_DIR = File("/home/jakub/Documents/Dev/BURP")
private val INPUT_DIR = ROOT_DIR.resolve("src/main/java")
private val OUTPUT_CSV = ROOT_DIR.resolve("exceptions_javaparser.csv")

data class Record(
    var engine: String = "BURP",
    var errorName: String = "",
    var errorDesc: String = "",
    var reference: String = ""
)

class ThrowVisitor(private val path: String, private val records: MutableList<Record>) :
    VoidVisitorAdapter<Void>() {
    override fun visit(n: ThrowStmt?, arg: Void?) {
        super.visit(n, arg)
        if (n == null) return
        val expr = n.expression
        var exName = ""
        var desc = ""
        if (expr is ObjectCreationExpr) {
            exName = expr.type.nameWithScope
            if (expr.arguments.size > 0) {
                val a0 = expr.getArgument(0)
                if (a0 is StringLiteralExpr) {
                    desc = a0.value
                }
            }
        } else {
            exName = expr?.javaClass?.simpleName ?: ""
        }
        val line = n.begin.map { it.line }.orElse(0)
        val rec = Record("BURP", exName, desc, "$path:$line")
        records.add(rec)
    }
}

fun escapeCsv(s: String?): String {
    if (s == null) return ""
    var out = s
    if (out.contains('"')) out = out.replace("\"", "\"\"")
    if (out.contains(',') || out.contains('\n') || out.contains('\r') || out.contains('"')) {
        out = "\"$out\""
    }
    return out
}

fun main() {
    val dir = INPUT_DIR
    val out = OUTPUT_CSV
    val records = mutableListOf<Record>()

    Files.walk(Paths.get(dir.toURI())).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".java") }
            .forEach { p ->
                try {
                    val cu = StaticJavaParser.parse(p.toFile())
                    cu.accept(ThrowVisitor(p.toString(), records), null)
                } catch (ex: Exception) {
                    // ignore parse errors for individual files
                }
            }
    }

    val outFile = out
    outFile.parentFile?.mkdirs()
    outFile.bufferedWriter(Charsets.UTF_8).use { w ->
        w.appendLine("Engine,ErrorName,ErrorDescription,Reference")
        for (r in records) {
            val relativeReference = r.reference.removePrefix(dir.absolutePath + "/")
            val row = listOf(r.engine, r.errorName, r.errorDesc, relativeReference)
                .joinToString(",") { escapeCsv(it) }
            w.appendLine(row)
        }
    }

    println("Wrote ${records.size} entries to ${outFile.absolutePath}")
}
