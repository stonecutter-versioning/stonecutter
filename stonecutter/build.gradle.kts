@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalPathApi::class)

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlin.io.path.ExperimentalPathApi

plugins {
    java
    signing
    `kotlin-dsl`
    `maven-publish`
    alias(common.plugins.gradle.shadow)
    alias(common.plugins.gradle.publish)
    alias(common.plugins.kotlin.jvm)
    alias(common.plugins.kotlin.dokka)
    alias(common.plugins.kotlin.dokka.javadoc)
    alias(common.plugins.kotlin.serialization)
    alias(common.plugins.kotlin.validator)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.kikugie.dev/releases")
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

sourceSets {
    main {
        files("src/main/entrypoint").builtBy(":updateVersion").let(kotlin::srcDir)
    }
}

dependencies {
    api(project(":stitcher"))
    api(common.misc.semver)
    api(common.misc.commons)
    implementation(common.kotlin.stdlib)
    implementation(common.kotlin.reflect)
    implementation(common.kotlin.serialization)
    implementation(common.kotlin.serialization.json)

    testImplementation(gradleTestKit())
    testImplementation(common.kotest.runner)
    testImplementation(common.kotest.assertions)

    testImplementation(common.ktor.json)
    testImplementation(common.ktor.client)
    testImplementation(common.ktor.client.java)
    testImplementation(common.ktor.client.negotiation)

    testImplementation(common.misc.mordant)

    dokkaHtmlPlugin(libs.dokka.versioning)
}

apiValidation {
    apiDumpDirectory = "src/api"
}

dokka {
    moduleName = "Stonecutter Gradle"

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }

    pluginsConfiguration.versioning {
        version = property("version").toString().substringBefore('-')
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
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.8/stonecutter/")
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
    withSourcesJar()
    withJavadocJar()
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    jvmToolchain(17)
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_2
        apiVersion = KotlinVersion.KOTLIN_2_2
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileTestKotlin {
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_2
            apiVersion = KotlinVersion.KOTLIN_2_2
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    publishPlugins {
        dependOnPublishTasks()
    }

    shadowJar {
        archiveClassifier = ""
        minimize()
        exclude("com/ibm/**", "org/jetbrains/**", "**/*.knm", "**/*.pro", "commonMain/**", "nativeMain/**")
    }

//    named<Jar>("javadocJar") {
//        from(named("dokkaGeneratePublicationJavadoc"))
//    }

    register<ShadowJar>("slimJar") {
        group = "build"
        archiveClassifier = "slim"
        configurations = project.configurations.runtimeClasspath.map(::listOf)

        from(sourceSets.main.map(SourceSet::getOutput))
        dependencies {
            include(project(":stitcher"))
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
            tags.addAll("preprocessor", "java", "kotlin", "resources")
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
