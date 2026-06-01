pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "BURP-Error"

include(":burp")
include(":turtleprov")
include(":rdf-object-loader")