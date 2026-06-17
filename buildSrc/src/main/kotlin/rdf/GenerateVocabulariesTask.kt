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

enum class OntologySpecification {
    RDFS, OWL2
}

abstract class GenerateVocabulariesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ontologyFiles: ConfigurableFileCollection

    @get:Input
    abstract val ontologySpecification: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val ontologyName: Property<String>

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val rdfLanguage: Property<String>

    @get:Input
    abstract val generateJenaModel: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val resourcePath: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "generation"
        description = "Generates a vocabulary object from ontology resources."
        rdfLanguage.convention(RDFLanguages.strLangTurtle)
        ontologySpecification.convention("OWL2")
        packageName.convention("burp.vocabularies")
        generateJenaModel.convention(false)
        resourcePath.convention("")
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/vocabulary/"))
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
            generateJenaModel = generateJenaModel.get(),
            resourcePath = resourcePath.get(),
            outFile = File(outDir, packageName.get().replace('.', '/') + "/${ontologyName.get()}.kt")
        )
    }

    private fun loadOntologyModel(files: List<File>, languageName: String): OntModel {
        val lang: Lang? = RDFLanguages.nameToLang(languageName)
        if (lang == null) {
            throw IllegalArgumentException("Unsupported RDF language: $languageName")
        }
        val ontologySpecification = when (ontologySpecification.get()) {
            "RDFS" -> OntSpecification.RDFS_MEM
            "OWL2" -> OntSpecification.OWL2_FULL_MEM
            else -> throw IllegalArgumentException("Unsupported ontology specification: ${ontologySpecification.get()}")
        }
        val model = OntModelFactory.createModel(ontologySpecification)
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
        generateJenaModel: Boolean,
        resourcePath: String,
        outFile: File,
    ) {
        outFile.parentFile?.mkdirs()

        // rdfs:Class
        // owl:Class
        val classes = model.classes().toList()
            .filter { it.uri?.startsWith(namespace) == true }
            .filter { it.localName != null }
            .distinctBy { it.uri }
            .sortedBy { it.uri }

        // rdf:Property
        // owl:ObjectProperty
        // owl:DatatypeProperty
        // owl:AnnotationProperty
        val properties = model.properties().toList()
            .filter { it.uri?.startsWith(namespace) == true }
            .filter { it.localName != null }
            .distinctBy { it.uri }
            .sortedBy { it.uri }

        // owl:NamedIndividual
        val individuals = model.individuals().toList()
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
            if (generateJenaModel) {
                if (resourcePath.isNotEmpty()) {
                    appendLine("import org.apache.jena.rdf.model.ModelFactory")
                    appendLine("import org.apache.jena.ontapi.OntModelFactory")
                    appendLine("import org.apache.jena.ontapi.OntSpecification")
                } else {
                    appendLine("import org.apache.jena.rdf.model.ResourceFactory")
                }
            }
            appendLine()
            appendLine("/** Generated vocabulary object for $ontologyName. */")
            appendLine("object $ontologyName {")
            appendLine("    const val NS: String = \"$namespace\"")
            appendLine()

            if (generateJenaModel) {
                if (resourcePath.isNotEmpty()) {
                    val ontologySpecification = when (ontologySpecification.get()) {
                        "RDFS" -> "OntSpecification.RDFS_MEM"
                        "OWL2" -> "OntSpecification.OWL2_FULL_MEM"
                        else -> throw IllegalArgumentException("Unsupported ontology specification: ${ontologySpecification.get()}")
                    }
                    appendLine("    private val M_MODEL = OntModelFactory.createModel($ontologySpecification, null).apply {")
                    appendLine("        read($ontologyName::class.java.classLoader.getResourceAsStream(\"$resourcePath\"), NS, \"TURTLE\")")
                    appendLine("    }")
                    appendLine()

                    for (cls in classes) {
                        val localName = cls.localName ?: continue
                        appendLine("    val ${renderIdentifier(localName)} = M_MODEL.createOntClass(\"$namespace$localName\")")
                    }

                    for (prop in properties) {
                        val localName = prop.localName ?: continue
                        appendLine("    val ${renderIdentifier(localName)} = M_MODEL.createProperty(\"$namespace$localName\")")
                    }

                    for (individual in individuals) {
                        val localName = individual.localName ?: continue
                        appendLine("    val ${renderIdentifier(localName)} = M_MODEL.createIndividual(\"$namespace$localName\")")
                    }
                } else {
                    appendLine("    private fun property(local: String) = ResourceFactory.createProperty(\"\${NS}\$local\")")
                    appendLine("    private fun resource(local: String) = ResourceFactory.createResource(\"\${NS}\$local\")")
                    appendLine()

                    for (cls in classes) {
                        val localName = cls.localName ?: continue
                        appendLine("    val ${renderIdentifier(localName)} = resource(\"$localName\")")
                    }

                    for (prop in properties) {
                        val localName = prop.localName ?: continue
                        appendLine("    val ${renderIdentifier(localName)} = property(\"$localName\")")
                    }

                    for (individual in individuals) {
                        val localName = individual.localName ?: continue
                        appendLine("    val ${renderIdentifier(localName)} = resource(\"$localName\")")
                    }
                }
            } else {
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
