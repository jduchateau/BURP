plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":rdf-object-loader"))
}
