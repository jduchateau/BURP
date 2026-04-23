package rml

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Cross-platform fetcher that downloads RML module repositories as ZIP archives
 * (no need for Git installed), extracts them, copies test-cases and shapes
 * into this project, and cleans up temporary files.
 *
 * Usage (recommended via Maven):
 *   mvn -q -DskipTests=true -Dexec.mainClass=burp.tools.FetchTestCases exec:java
 */
object FetchTestCases {

    @JvmStatic
    fun main(args: Array<String>) {
        val root = Paths.get(".")
        run(
            testCasesDir = root.resolve("src/test/resources"),
            shapesDir = root.resolve("src/main/resources/shapes"),
            vocabulariesDir = root.resolve("src/main/resources/vocabularies/rml/")
        )
    }

    fun run(testCasesDir: Path?, shapesDir: Path?, vocabulariesDir: Path?) {

        val repos = listOf(
            "rml-core",
            "rml-cc",
            "rml-io",
            "rml-fnml",
            "rml-lv",
            "rml-io-registry"
        )

        for (repo in repos) {
            val base = "https://github.com/kg-construct/$repo"
            val zipMain = "$base/archive/refs/heads/main.zip"
            val zipMaster = "$base/archive/refs/heads/master.zip"

            try {
                println("Downloading and extracting $repo …")
                val tempDir = Files.createTempDirectory("burp-fetch-$repo")

                try {
                    if (!downloadWithFallback(zipMain, zipMaster, tempDir)) {
                        System.err.println("• Failed to download $repo (main/master not found). Skipping.")
                        continue
                    }

                    // GitHub zip contains a single top-level directory named like <repo>-<branch>
                    val innerRoot = Files.list(tempDir).use { stream ->
                        stream.filter { Files.isDirectory(it) }.findFirst().orElse(null) ?: tempDir
                    }

                    // Copy test-cases
                    val srcTestCases = innerRoot.resolve("test-cases")
                    val destTestCasesRoot = testCasesDir
                    if (destTestCasesRoot == null) {
                        println("• Skipped $repo test-cases (destination not configured)")
                    } else if (Files.isDirectory(srcTestCases)) {
                        val destTestCases = destTestCasesRoot.resolve(repo)
                        Files.createDirectories(destTestCases)
                        copyDir(srcTestCases, destTestCases)
                        println("• Copied test-cases: $srcTestCases -> $destTestCases")
                    } else {
                        println("• Skipped $repo (no test-cases directory found)")
                    }

                    // Copy shapes
                    val srcShapes = innerRoot.resolve("shapes")
                    val destShapesRoot = shapesDir
                    if (destShapesRoot == null) {
                        println("• Skipped $repo shapes (destination not configured)")
                    } else if (Files.isDirectory(srcShapes)) {
                        val destShapes = destShapesRoot.resolve(repo)
                        Files.createDirectories(destShapes)
                        copyDir(srcShapes, destShapes)
                        println("• Copied shapes: $srcShapes -> $destShapes")
                    } else {
                        println("• Skipped $repo (no shapes directory found)")
                    }

                    // Copy vocabulary
                    val srcVoc = innerRoot.resolve("ontology/$repo.owl")
                    val destVocabRoot = vocabulariesDir
                    if (destVocabRoot == null) {
                        println("• Skipped $repo vocabulary (destination not configured)")
                    } else if (Files.exists(srcVoc)) {
                        val destVoc = destVocabRoot.resolve("$repo.owl")
                        Files.createDirectories(destVocabRoot)
                        Files.copy(srcVoc, destVoc, StandardCopyOption.REPLACE_EXISTING)
                        println("• Copied vocabulary: $srcVoc -> $destVoc")
                    } else {
                        println("• Skipped $repo (no ontology file found)")
                    }


                } finally {
                    // Cleanup extracted content
                    safeDeleteRecursively(tempDir)
                }
            } catch (e: Exception) {
                System.err.println()
                System.err.println("Error occurred while processing $repo.")
                System.err.println("• Check the repository URLs and your internet connection.")
                System.err.println("Details: ${e.message}")
            }
        }

        println("Done.")
    }



    private fun downloadWithFallback(url1: String, url2: String, dest: Path): Boolean {
        return try {
            downloadAndExtract(url1, dest); true
        } catch (_: IOException) {
            try {
                downloadAndExtract(url2, dest); true
            } catch (_: IOException) {
                false
            }
        }
    }

    private fun downloadAndExtract(urlStr: String, destDir: Path): Boolean {
        return try {
            val url = URI(urlStr).toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = Duration.ofSeconds(20).toMillis().toInt()
                readTimeout = Duration.ofSeconds(60).toMillis().toInt()
                requestMethod = "GET"
                setRequestProperty("Accept", "application/zip")
            }
            if (conn.responseCode !in 200..299) {
                conn.inputStream?.close()
                return false
            }

            // Stream directly from HTTP to ZIP extraction
            Files.createDirectories(destDir)
            BufferedInputStream(conn.inputStream).use { input ->
                unzipStream(input, destDir)
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun unzipStream(input: InputStream, destDir: Path) {
        ZipInputStream(input).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outPath = destDir.resolve(entry.name).normalize()
                // Prevent Zip Slip
                if (!outPath.startsWith(destDir)) {
                    throw IOException("Bad zip entry: ${entry.name}")
                }
                if (entry.isDirectory) {
                    Files.createDirectories(outPath)
                } else {
                    Files.createDirectories(outPath.parent)
                    copyStream(zis, outPath)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }


    private fun copyStream(input: InputStream, dest: Path) {
        Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { out ->
            input.copyTo(out)
        }
    }

    private fun copyDir(src: Path, dest: Path) {
        Files.walkFileTree(src, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val target = dest.resolve(src.relativize(dir))
                Files.createDirectories(target)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val target = dest.resolve(src.relativize(file))
                Files.createDirectories(target.parent)
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun safeDeleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.deleteIfExists(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.deleteIfExists(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }
}

abstract class FetchTestCasesTask : DefaultTask() {

    @get:Optional
    @get:OutputDirectory
    abstract val testCasesDirectory: DirectoryProperty

    @get:Optional
    @get:OutputDirectory
    abstract val shapesDirectory: DirectoryProperty

    @get:Optional
    @get:OutputDirectory
    abstract val vocabulariesDirectory: DirectoryProperty

    init {
        testCasesDirectory.convention(project.layout.projectDirectory.dir("src/test/resources"))
        shapesDirectory.convention(project.layout.projectDirectory.dir("src/main/resources/shapes"))
        vocabulariesDirectory.convention(project.layout.projectDirectory.dir("src/main/resources/vocabularies/rml/"))
        group = "verification"
        description = "Downloads and refreshes external RML test-cases before tests."
    }

    @TaskAction
    fun fetch() {
        FetchTestCases.run(
            testCasesDir = testCasesDirectory.orNull?.asFile?.toPath(),
            shapesDir = shapesDirectory.orNull?.asFile?.toPath(),
            vocabulariesDir = vocabulariesDirectory.orNull?.asFile?.toPath()
        )
    }
}

