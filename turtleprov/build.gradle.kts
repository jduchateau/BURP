import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import rdf.GenerateVocabulariesTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.strumenta.antlr-kotlin") version "1.0.8"
    alias(libs.plugins.ksp)

    kotlin("npm-publish") version "3.7.0"
    alias(libs.plugins.vanniktech.mavenPublish)
}

repositories {
    mavenCentral()
}

val generateRdfTestVocabulary = tasks.register<GenerateVocabulariesTask>("generateRdfTestVocabulary") {
    ontologyFiles = layout.projectDirectory.dir("src/commonTest/resources/rdf-tests/ns/").asFileTree.matching {
        include("*.ttl")
    }
    ontologySpecification = "RDFS"
    ontologyName = "RdfTest"
    namespace = "http://www.w3.org/ns/rdftest#"
    outputDirectory = layout.buildDirectory.dir("generated/vocabulary/").get().asFile
    packageName = "turtleprov.manifest.gen"
    rdfLanguage = "TURTLE"
}

val generateRdfManifestVocabulary = tasks.register<GenerateVocabulariesTask>("generateRdfManifestVocabulary") {
    ontologyFiles =
        layout.projectDirectory.dir("src/commonTest/resources/rdf-tests/ns/").asFileTree.matching {
            include("*.ttl")
        }
    ontologySpecification = "RDFS"
    ontologyName = "RdfManifest"
    namespace = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#"
    outputDirectory = layout.buildDirectory.dir("generated/vocabulary/").get().asFile
    packageName = "turtleprov.manifest.gen"
    rdfLanguage = "TURTLE"
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr4")) { include("**/*.g4") }
    packageName = "turtleprov.generated"
    arguments = listOf("-visitor", "-no-listener")
    outputDirectory = layout.buildDirectory.dir("generated/antlr/turtleprov").get().asFile
}

kotlin {
    jvm {
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    jvmToolchain(17)
    js(IR) {
        nodejs()
        binaries.library()
    }

    sourceSets {
        commonMain {
            kotlin {
                srcDir(generateKotlinGrammarSource)
                srcDir(generateRdfTestVocabulary)
                srcDir(generateRdfManifestVocabulary)
            }
            dependencies {
                implementation("com.strumenta:antlr-kotlin-runtime:1.0.8")
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
                implementation(project(":rdf-object-loader"))
                implementation(project(":rdf-model"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jsTest.dependencies {
            implementation(npm("rdf-isomorphic", "^1.3.1"))
        }

        jvmTest.dependencies {
            implementation(libs.jena.arq)
        }
    }
}

mavenPublishing {
    pom {
        description =
            "A Kotlin Multiplatform RDF Turtle parser that preserves provenance information, tracking source node positions for subjects, predicates, and objects"
    }
}

dependencies {
    add("kspJvm", project(":rdf-object-loader-processor"))
    add("kspJs", project(":rdf-object-loader-processor"))
}
