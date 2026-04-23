plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    implementation(libs.jena.arq)
    implementation(libs.jena.cmds)
    implementation(libs.jena.ontapi)
}

