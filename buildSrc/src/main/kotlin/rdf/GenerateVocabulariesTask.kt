package rdf

import org.apache.jena.ontapi.OntModelFactory
import org.apache.jena.ontapi.OntSpecification
import org.apache.jena.ontapi.model.OntModel
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.vocabulary.OWL2
import org.apache.jena.vocabulary.RDF
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.charset.StandardCharsets

abstract class GenerateVocabulariesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ontologyFiles: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val ontologyName: Property<String>

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val rdfLanguage: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "generation"
        description = "Generates a vocabulary object from ontology resources."
        rdfLanguage.convention(RDFLanguages.strLangTurtle)
    }

    @TaskAction
    fun generate() {
        val outDir = outputDirectory.get().asFile
        outDir.mkdirs()

        if (ontologyFiles.isEmpty) {
            throw StopExecutionException("No ontology files found.")
        }

        val model = loadOntologyModel(ontologyFiles.files.sortedBy { it.name }, rdfLanguage.get())
        generateVocabulary(
            model = model,
            packageName = packageName.get(),
            ontologyName = ontologyName.get(),
            namespace = namespace.get(),
            outFile = File(outDir, packageName.get().replace('.', '/') + "/${ontologyName.get()}.kt")
        )
    }

    private fun loadOntologyModel(files: List<File>, languageName: String): OntModel {
        val lang: Lang? = RDFLanguages.nameToLang(languageName)
        if (lang == null) {
            throw IllegalArgumentException("Unsupported RDF language: $languageName")
        }
        val model = OntModelFactory.createModel(OntSpecification.OWL2_FULL_MEM)
        for (file in files) {
            file.inputStream().use { input ->
                RDFDataMgr.read(model, input, null, lang)
            }
        }
        return model
    }

    private fun generateVocabulary(
        model: OntModel,
        packageName: String,
        ontologyName: String,
        namespace: String,
        outFile: File,
    ) {
        outFile.parentFile?.mkdirs()

        val classes = model.classes()
            .filter { it.uri?.startsWith(namespace) == true }
            .filter { it.localName != null }
            .toList()
            .distinctBy { it.uri }
            .sortedBy { it.uri }

        val properties = model.properties()
            .filter { it.uri?.startsWith(namespace) == true }
            .filter { it.localName != null }
            .toList()
            .distinctBy { it.uri }
            .sortedBy { it.uri }

        val individuals = model.listSubjectsWithProperty(RDF.type, OWL2.NamedIndividual)
            .toList()
            .asSequence()
            .filter { it.uri?.startsWith(namespace) == true }
            .filter { it.localName != null }
            .distinctBy { it.uri }
            .sortedBy { it.uri }
            .toList()

        if (classes.isEmpty() && properties.isEmpty() && individuals.isEmpty()) {
            throw IllegalArgumentException("No classes, properties, or individuals found in the ontology with namespace $namespace")
        }

        val text = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import rdf.NamedTerm")
            appendLine()
            appendLine("/** Generated vocabulary object for $ontologyName. */")
            appendLine("object $ontologyName {")
            appendLine("    const val NS: String = \"$namespace\"")
            appendLine()

            for (cls in classes) {
                val localName = cls.localName ?: continue
                appendLine("    const val ${renderIdentifier(localName)} = \"$namespace$localName\"")
            }

            for (prop in properties) {
                val localName = prop.localName ?: continue
                appendLine("    const val ${renderIdentifier(localName)} = \"$namespace$localName\"")
            }

            for (individual in individuals) {
                val localName = individual.localName ?: continue
                appendLine("    const val ${renderIdentifier(localName)} = \"$namespace$localName\"")
            }

            appendLine("}")
        }

        outFile.writeText(text, StandardCharsets.UTF_8)
    }

    private fun renderIdentifier(name: String): String {
        val sanitized = buildString {
            name.forEachIndexed { index, ch ->
                val allowed = ch.isLetterOrDigit() || ch == '_'
                when {
                    allowed -> append(ch)
                    index == 0 -> append('_')
                    else -> append('_')
                }
            }
        }.let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

        return if (sanitized in kotlinKeywords) "`$sanitized`" else sanitized
    }

    private companion object {
        val kotlinKeywords = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "val", "var", "when", "while"
        )
    }
}

