import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(common.plugins.kotlin.jvm)
    alias(common.plugins.kotlin.dokka)
    alias(common.plugins.kotlin.serialization)
}

version = "SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.kikugie.dev/releases")
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    api("dev.kikugie:semver:2.0.0")
    api("dev.kikugie:commons:0.3.1")
    implementation(common.kotlin.reflect)
    implementation(common.kotlin.serialization)

    testImplementation(kotlin("test"))
    testImplementation(common.kotlin.serialization.yaml)
}

dokka {
    moduleName = "Stitcher Processor"
    dokkaPublications.html {
        suppressInheritedMembers = true
        suppressObviousFunctions = true
    }

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }

    dokkaSourceSets.named("main") {
        reportUndocumented = false
        skipEmptyPackages = true

        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.7/stitcher/")
        }

        externalDocumentationLinks.register("kotlin-stdlib") {
            url("https://kotlinlang.org/api/core/")
        }

        externalDocumentationLinks.register("kotlinx-serialization") {
            url("https://kotlinlang.org/api/kotlinx.serialization/")
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<KotlinCompile> {
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_0
            apiVersion = KotlinVersion.KOTLIN_2_0
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
