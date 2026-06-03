plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
    jvm()
    js {
        nodejs()
        binaries.library()
    }


    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
            implementation(project(":rdf-model"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation(libs.kotlin.reflect)
            implementation(libs.jena.arq)
        }
    }
}

dependencies {
    add("kspJvmTest", project(":rdf-object-loader-processor"))
    add("kspJsTest", project(":rdf-object-loader-processor"))
}

mavenPublishing {
    pom {
        description = "A Kotlin Multiplatform RDF-to-object mapper and loader supporting declarative mapping with annotations"
    }
}
