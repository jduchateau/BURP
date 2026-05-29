import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import rml.FetchTestCasesTask

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinKapt)
    application
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
}

group = "be.uliege.dre"
version = "0.1.7"

val generatedVocabularyDir = layout.buildDirectory.dir("generated/source/schemagen")

val jenaSchemagen by configurations.creating

dependencies {
    add(jenaSchemagen.name, libs.jena.cmds)
}

val generateRerVocabulary = tasks.register<JavaExec>("generateRerVocabulary") {
    group = "build setup"
    description = "Generates the RER vocabulary constants with Jena schemagen."
    classpath = jenaSchemagen
    mainClass.set("jena.schemagen")

    val rerInput = layout.projectDirectory.file("src/main/resources/vocabularies/rer.ttl").asFile
    val ptrImport = layout.projectDirectory.file("src/main/resources/vocabularies/ptr.ttl").asFile
    val outputDir = generatedVocabularyDir.get().asFile

    inputs.file(rerInput)
    inputs.file(ptrImport)
    outputs.dir(outputDir)

    args(
        "-i", rerInput.absolutePath,
        "-e", "TURTLE",
        "-o", outputDir.absolutePath,
        "--package", "burp.vocabularies",
        "-n", "RER",
        "--ontology",
        "--rdfs",
        "--inference",
        "--import", ptrImport.absolutePath,
        "-a", "https://w3id.org/dre/rer#",
    )
}

val generatePtrVocabulary = tasks.register<JavaExec>("generatePtrVocabulary") {
    group = "build setup"
    description = "Generates the PTR vocabulary constants with Jena schemagen."
    classpath = jenaSchemagen
    mainClass.set("jena.schemagen")

    val ptrInput = layout.projectDirectory.file("src/main/resources/vocabularies/ptr.ttl").asFile
    val outputDir = generatedVocabularyDir.get().asFile

    inputs.file(ptrInput)
    outputs.dir(outputDir)

    args(
        "-i", ptrInput.absolutePath,
        "-e", "TURTLE",
        "-o", outputDir.absolutePath,
        "--package", "burp.vocabularies",
        "-n", "PTR",
        "--ontology",
        "--rdfs",
        "--inference",
        "-a", "https://w3id.org/dre/ptr#",
    )
}

val generateVocabularies = tasks.register("generateVocabularies") {
    dependsOn(generateRerVocabulary, generatePtrVocabulary)
}

val fetchTestCases = tasks.register<FetchTestCasesTask>("fetchTestCases")
fetchTestCases.configure {
    description = "Fetches only test-cases into test resources."
    shapesDirectory.unset()
    shapesDirectory.unsetConvention()
    vocabulariesDirectory.unset()
    vocabulariesDirectory.unsetConvention()
}

val fetchVocabularyAndShapes = tasks.register<FetchTestCasesTask>("fetchVocabularyAndShapes")
fetchVocabularyAndShapes.configure {
    description = "Fetches only vocabularies and shapes into main resources."
    testCasesDirectory.unset()
    testCasesDirectory.unsetConvention()
}


application {
    applicationName = "burp"
    mainClass.set("burp.Main")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin {
                srcDir(generatedVocabularyDir)
            }
        }
    }
}


dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.strumenta:antlr-kotlin-runtime:1.0.8")
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation("at.asitplus:jsonpath4k:3.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
    implementation("org.mongodb:bson:5.6.5")

    implementation(project(":turtleprov"))


    implementation(libs.jena.arq)
    implementation(libs.jena.shacl)
    implementation(libs.jena.iri3986)
    implementation(libs.jena.ontapi)
    implementation("com.opencsv:opencsv:5.12.0")
    implementation("com.univocity:univocity-parsers:2.9.1")
    implementation("com.jayway.jsonpath:json-path:2.10.0")
    implementation("net.minidev:json-smart:2.6.0")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.microsoft.sqlserver:mssql-jdbc:13.2.1.jre11")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("net.sf.saxon:Saxon-HE:12.9")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("org.tukaani:xz:1.9")

    compileOnly(libs.google.auto.service.annotations)
    kapt(libs.google.auto.service.processor)
    annotationProcessor(libs.google.auto.service.processor) // For Java annotation processing


    // implementation(libs.ktor.server.core)
    // implementation(libs.ktor.server.netty)
    // implementation(libs.logback.classic)
    // implementation(libs.ktor.server.core)


    testImplementation(kotlin("test"))

    // testImplementation(libs.org.junit.jupiter.junit.jupiter.api)
    testImplementation(libs.junit.params)
    // testRuntimeOnly(libs.org.junit.jupiter.junit.jupiter.engine)
    // testRuntimeOnly(libs.org.junit.platform.junit.platform.launcher)

    testImplementation(libs.ktor.server.test.host)

    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.mssqlserver)
    testImplementation(libs.jena.fuseki.main)
}


tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateVocabularies)
    source(generatedVocabularyDir)
}

tasks.processTestResources {
    dependsOn(fetchTestCases)
}

tasks.processResources {
    dependsOn(fetchVocabularyAndShapes)
}

tasks.compileKotlin {
    dependsOn(generateVocabularies)
}

tasks.withType<KaptGenerateStubsTask>().configureEach {
    dependsOn(generateRerVocabulary, generatePtrVocabulary)
}

tasks.shadowJar {
    archiveFileName = "burp.jar"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

