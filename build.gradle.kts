import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import rml.FetchTestCasesTask
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jreleaser.model.Active
import org.jreleaser.model.Distribution.DistributionType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    id("com.strumenta.antlr-kotlin") version "1.0.8"
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.jreleaser)
    id("com.gradleup.shadow") version "9.4.1"
    application
    signing
}

group = "io.github.jduchateau"

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")
    source = fileTree(layout.projectDirectory.dir("src/main/antlr4")) { include("**/*.g4") }
    packageName = "turtleprov.generated"
    arguments = listOf("-visitor", "-no-listener")
    outputDirectory = layout.buildDirectory.dir("generated/antlr/turtleprov").get().asFile
}

val generatedVocabularyDir = layout.buildDirectory.dir("generated/schemagen")

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
        "--declarations",
        "static { M_MODEL.read(RER.class.getClassLoader().getResourceAsStream(\"vocabularies/rer.ttl\"), RER.NS, \"TURTLE\"); }"
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
//        "--declarations",
//        "static { M_MODEL.read(PTR.class.getClassLoader().getResourceAsStream(\"vocabularies/ptr.ttl\"), PTR.NS, \"TURTLE\"); }"
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


kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin {
                srcDir(generatedVocabularyDir)
                srcDir(generateKotlinGrammarSource)
            }
        }
    }
}

application {
    applicationName = "burp"
    mainClass = "burp.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}


dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.strumenta:antlr-kotlin-runtime:1.0.8")
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation("at.asitplus:jsonpath4k:3.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
    implementation("org.mongodb:bson:5.6.5")


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

    // testImplementation(libs.ktor.server.test.host)

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
    dependsOn(generateKotlinGrammarSource, generateVocabularies)
}

tasks.withType<KaptGenerateStubsTask>().configureEach {
    dependsOn(generateKotlinGrammarSource, generateRerVocabulary, generatePtrVocabulary)
}

tasks.shadowJar {
    archiveFileName = "burp.jar"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}



signing {
    isRequired = System.getenv("CI") != null
}

mavenPublishing {
    val gitlabProjectId: String = System.getenv("CI_PROJECT_ID") ?: "8659"
    val gitlabToken: String? = System.getenv("CI_JOB_TOKEN") ?: System.getenv("GITLAB_TOKEN")

    coordinates("io.github.jduchateau", project.name, project.version.toString())

    pom {
        name.set(project.name)
        inceptionYear.set("2024")
        url.set("https://github.com/jduchateau/BURP-Errors/")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/license/mit/")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("jduchateau")
                name.set("Jakub Duchateau")
                organization.set("University of Liège")
                organizationUrl.set("https://www.uliege.be/")
            }
        }
        scm {
            url.set("https://github.com/jduchateau/BURP-Errors/")
            connection.set("scm:git:git://github.com/jduchateau/BURP-Errors.git")
            developerConnection.set("scm:git:ssh://git@github.com/jduchateau/BURP-Errors.git")
        }
    }

    repositories {
        mavenCentral()
        maven {
            name = "Gitlab"
            url = uri("https://gitlab.uliege.be/api/v4/projects/$gitlabProjectId/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = gitlabToken
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jduchateau/BURP-Errors")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: System.getenv("GITHUB_USER")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

jreleaser {
    project {
        description = "A Basic and Unassuming RML Processor (BURP) with RML Execution Report (RER) error handling"
        version = project.version.toString()
        authors = listOf("Jakub Duchateau")
        license = "MIT"
        links {
            homepage = "https://github.com/jduchateau/BURP-Errors"
        }
        copyright = "2026 Jakub Duchateau, 2024 Christophe Debruyne"
    }

    release {
        github {
            repoOwner = "jduchateau"
            name = "BURP-Errors"
            host = "github.com"
            overwrite = true
            skipTag = true
            draft = true
            changelog {
                enabled = true
                formatted = Active.ALWAYS
                preset = "conventional-commits"
            }
            issues {
                enabled = true

            }
        }
    }




    distributions {
        create("burp") {
            active.set(Active.ALWAYS)
            distributionType.set(DistributionType.SINGLE_JAR)
            java {
                version.set("21")
                mainClass.set("burp.Main")
            }
            executable {
                name.set("burp")
            }
            artifact {
                path.set(layout.buildDirectory.file("libs/burp.jar"))
            }
        }
    }

    packagers {
        jbang {
            active.set(Active.ALWAYS)
            repository {
                active.set(Active.ALWAYS)
                repoOwner.set("jduchateau")
                name.set("jbang-catalog")
            }
            commitAuthor {
                name.set("jduchateau")
                email.set("jduchateau@users.noreply.github.com")
            }
        }
    }
}
