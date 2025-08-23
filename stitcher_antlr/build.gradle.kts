import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    antlr
    alias(common.plugins.kotlin.jvm)
    alias(common.plugins.kotlin.dokka)
}

group = "dev.kikugie"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.kikugie.dev/releases")
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    api("dev.kikugie:semver:2.0.0")
    api("dev.kikugie:commons:0.3.1")
    implementation(common.misc.mordant)
    testImplementation(common.kotest.runner)
    testImplementation(common.kotest.assertions)
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
//        documentedVisibilities = setOf(VisibilityModifier.Public, VisibilityModifier.Internal)

        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.8/stitcher_antlr/")
        }

        externalDocumentationLinks.register("antlr") {
            url("https://javadoc.io/doc/org.antlr/antlr4-runtime/latest/index.html")
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

    generateGrammarSource {
        arguments = arguments + listOf("-visitor", "-no-listener", "-long-messages", "-lib", "$projectDir/src/main/antlr/dev/kikugie/stitcher/antlr", "-lib", "$projectDir/src/main/antlr/dev/kikugie/stitcher/antlr/scanner")
    }

    compileKotlin {
        dependsOn(generateGrammarSource)
    }

    compileTestKotlin {
        dependsOn(generateTestGrammarSource)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
    explicitApiWarning()

    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_2
        apiVersion = KotlinVersion.KOTLIN_2_2

        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xnested-type-aliases", "-Xcontext-sensitive-resolution", "-Xwhen-guards")
    }
}