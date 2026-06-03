import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.strumenta.antlr-kotlin") version "1.0.8"
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)

    kotlin("npm-publish") version "3.7.0"
    alias(libs.plugins.vanniktech.mavenPublish)
}

repositories {
    mavenCentral()
}
val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")
    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr4")) { include("**/*.g4") }
    packageName = "turtleprov.generated"
    arguments = listOf("-visitor", "-no-listener")
    outputDirectory = layout.buildDirectory.dir("generatedAntlr/turtleprov").get().asFile
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
            }
            dependencies {
                implementation("com.strumenta:antlr-kotlin-runtime:1.0.8")
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
                implementation(project(":rdf-object-loader"))
                implementation(project(":rdf-model"))
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }

        jsTest.dependencies {
            implementation(npm("rdf-isomorphic", "^1.3.1"))
        }

        jvmTest.dependencies {
            implementation(libs.jena.arq)
            implementation(libs.kotest.runner.junit5)
        }
    }
}

mavenPublishing {
    pom {
        description =
            "A Kotlin Multiplatform RDF Turtle parser that preserves provenance information, tracking source node positions for subjects, predicates, and objects"
    }
}
