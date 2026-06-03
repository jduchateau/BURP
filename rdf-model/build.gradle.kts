plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jsMain.dependencies {
            implementation(npm("n3", "^1.17.3"))
        }
        jvmMain.dependencies {
            implementation(libs.jena.arq)
        }
    }
}

mavenPublishing {
    pom {
        description = "A lightweight Kotlin Multiplatform RDF model library providing terms, quads, and dataset structures, interoperable with Jena (jvm) and n3 (js)"
    }
}
