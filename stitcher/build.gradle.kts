import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.serialization)
}

version = "SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    api(project(path = ":semver"))
    implementation(kotlin("reflect"))
    implementation(libs.bundles.stitcher)

    testImplementation(libs.junit.api)
    testImplementation(libs.extra.mordant)
    testImplementation(libs.kotlin.serialization.yaml)
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
//    withType<Test> {
//        useJUnitPlatform()
//    }

    withType<KotlinCompile> {
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_1
            apiVersion = KotlinVersion.KOTLIN_2_1
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
