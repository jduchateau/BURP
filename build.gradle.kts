import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinKapt) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}

group = "be.uliege.dre"
version = "0.1.0"

val githubUser: String? = System.getenv("GITHUB_ACTOR") ?: findProperty("githubUser")?.toString()
val githubToken: String? = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken")?.toString()

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/jduchateau/BURP-Error")
                    credentials {
                        username = githubUser ?: ""
                        password = githubToken ?: ""
                    }
                }
            }

            coordinates(group.toString(), project.name, version.toString())

            pom {
                url = "https://github.com/jduchateau/BURP-Errors/"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/license/mit/"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "jduchateau"
                        name = "Jakub Duchateau"
                        organization = "University of Liège"
                        organizationUrl = "https://www.uliege.be/"
                    }
                }
                scm {
                    url = "https://github.com/jduchateau/BURP-Errors/"
                    connection = "scm:git:git://github.com/jduchateau/BURP-Errors.git"
                    developerConnection = "scm:git:ssh://git@github.com/jduchateau/BURP-Errors.git"
                }
            }
        }
    }
}

