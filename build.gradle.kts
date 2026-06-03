import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jreleaser.model.Active
import org.jreleaser.model.Distribution.DistributionType

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinKapt) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.jreleaser)
    base
}

group = "io.github.jduchateau"

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        val gitlabProjectId: String = System.getenv("CI_PROJECT_ID") ?: "8659"
        val gitlabToken: String? = System.getenv("CI_JOB_TOKEN") ?: System.getenv("GITLAB_TOKEN")

        plugins.withId("signing") {
            configure<SigningExtension> {
                isRequired = System.getenv("CI") != null
            }
        }

        configure<MavenPublishBaseExtension> {
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

            coordinates("io.github.jduchateau", project.name, project.version.toString())

            pom {
                name.set(project.name)
                inceptionYear.set("2025")
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
        }
    }
}

jreleaser {
    project {
        description = "A Basic and Unassuming RML Processor (BURP) with RML Execution Report (RER) error handling"
        version = childProjects["burp"]!!.version.toString()
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
                path.set(file("burp/build/libs/burp.jar"))
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



