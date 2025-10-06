import org.gradle.kotlin.dsl.antlr
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    antlr
    alias(common.plugins.kotlin.jvm)
    alias(common.plugins.kotlin.dokka)
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.kikugie.dev/releases")
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    antlr(libs.antlr)
    compileOnly(common.misc.mordant)
    implementation(libs.ahocorasick)
    implementation(common.misc.semver)
    implementation(common.misc.commons)
    implementation(common.kotlin.stdlib)
    implementation(common.kotlin.reflect)
    testImplementation(common.kotest6.runner)
    testImplementation(common.kotest6.assertions)

    dokkaHtmlPlugin(libs.dokka.versioning)
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

    pluginsConfiguration.versioning {
        version = property("version").toString().substringBefore('-')
    }

    dokkaSourceSets.named("main") {
        reportUndocumented = false
        skipEmptyPackages = true
//        documentedVisibilities = setOf(VisibilityModifier.Public, VisibilityModifier.Internal)

        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteLineSuffix = "#L"
            remoteUrl("https://codeberg.org/stonecutter/stonecutter/src/branch/0.8/stitcher/")
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
    jvmToolchain(17)
    explicitApiWarning()

    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_2
        apiVersion = KotlinVersion.KOTLIN_2_2
    }
}