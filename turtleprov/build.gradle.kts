import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask

repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("npm-publish") version "3.7.0"
    id("com.strumenta.antlr-kotlin") version "1.0.8"
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")
    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr4")) { include("**/*.g4") }
    packageName = "turtleprov.generated"
    arguments = listOf("-visitor", "-no-listener")
    outputDirectory = layout.buildDirectory.dir("generatedAntlr/turtleprov").get().asFile
}

kotlin {
    jvm()
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
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


npmPublish {
    registries {
        register("github") {
            uri.set("https://npm.pkg.github.com/")
            authToken.set(System.getenv("GITHUB_TOKEN") ?: "")
        }
    }
}
