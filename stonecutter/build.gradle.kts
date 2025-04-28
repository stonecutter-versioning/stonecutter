@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalPathApi::class)

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.jvm.tasks.Jar
import kotlin.io.path.ExperimentalPathApi

plugins {
    idea
    java
    signing
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.dokka.javadoc)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.validator)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        files("src/main/entrypoint").builtBy(":updateVersion").let(kotlin::srcDir)
    }
}

dependencies {
//    api(project(path = ":semver"))
    api(project(path = ":stitcher"))
    implementation(libs.bundles.stonecutter)
}

apiValidation {
    ignoredPackages += "stonecutter_samples"
    nonPublicMarkers += "dev.kikugie.stonecutter.StonecutterInternalAPI"
}

dokka {
    moduleName = "Stonecutter Gradle"

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }

    dokkaPublications.all {
        suppressInheritedMembers = true
        suppressObviousFunctions = true
    }

    dokkaSourceSets.named("main") {
        reportUndocumented = true
        skipEmptyPackages = true

        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.7/stonecutter/")
        }

        externalDocumentationLinks.register("gradle-kotlin-dsl") {
            url("https://docs.gradle.org/current/kotlin-dsl/")
            packageListUrl("https://docs.gradle.org/current/kotlin-dsl/gradle/package-list")
        }

        externalDocumentationLinks.register("kotlin-stdlib") {
            url("https://kotlinlang.org/api/core/")
        }

        externalDocumentationLinks.register("kotlinx-serialization") {
            url("https://kotlinlang.org/api/kotlinx.serialization/")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

tasks {
    register<ShadowJar>("slimJar") {
        group = "build"
        archiveClassifier = "slim"
        configurations = project.configurations.runtimeClasspath.map(::listOf)

        from(sourceSets.main.map(SourceSet::getOutput))
        dependencies {
//            include(project(":semver"))
            include(project(":stitcher"))
        }
    }

    named<Jar>("javadocJar") {
        from(named("dokkaGeneratePublicationJavadoc"))
    }

    shadowJar {
        archiveClassifier = ""
    }

    compileKotlin {
        explicitApiMode = ExplicitApiMode.Strict
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_1
            apiVersion = KotlinVersion.KOTLIN_2_1
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

gradlePlugin {
    website = "https://stonecutter.codeberg.page/"
    vcsUrl = "https://codeberg.org/stonecutter/stonecutter"

    plugins {
        create("stonecutter") {
            id = "dev.kikugie.stonecutter"
            implementationClass = "dev.kikugie.stonecutter.StonecutterPlugin"
            displayName = "Stonecutter"
            description = "Modern Gradle plugin for multi-version management"
            tags = listOf("stonecutter")
        }
    }
}

publishing {
    repositories {
        fun register(type: String, build: MavenArtifactRepository.() -> Unit) {
            val username = findProperty("mvn.$type.username") as String?
            val password = findProperty("mvn.$type.password") as String?
            if (username == null || password == null)
                return println("Missing credentials for $type maven repository")

            maven {
                build()
                credentials {
                    this.username = username
                    this.password = password
                }
            }
        }

        register("kikugie") {
            name = "KikuGieMaven"
            url = when {
                '-' in project.version.toString() -> uri("https://maven.kikugie.dev/snapshots")
                else -> uri("https://maven.kikugie.dev/releases")
            }
        }
    }

    publications {
        register<MavenPublication>("maven") {
            groupId = "dev.kikugie"
            artifactId = "stonecutter"
            version = project.version.toString()
            from(components["java"])
            artifact(tasks.named("slimJar"))

            pom {
                name = "Stonecutter"
                description = "Modern Gradle plugin for multi-version management"
                url = "https://stonecutter.kikugie.dev/"

                developers {
                    developer {
                        id = "kikugie"
                        name = "KikuGie"
                        email = "kikugie@duck.com"
                    }
                }

                licenses {
                    license {
                        name = "GNU Lesser Public License 3.0"
                        url = "https://www.gnu.org/licenses/lgpl-3.0.en.html"
                    }
                }

                scm {
                    connection = "scm:git:git:https://codeberg.org/stonecutter/stonecutter.git"
                    developerConnection = "scm:git:ssh://codeberg.org:stonecutter/stonecutter.git"
                    url = "https://codeberg.org/stonecutter/stonecutter"
                }
            }
        }
    }
}

signing {
    sign(configurations.runtimeElements.get())
}
