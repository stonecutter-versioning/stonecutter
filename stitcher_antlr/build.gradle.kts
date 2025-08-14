import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    antlr
    alias(common.plugins.kotlin.jvm)
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

    withType<KotlinCompile> {
        compilerOptions {
            languageVersion = KotlinVersion.KOTLIN_2_2
            apiVersion = KotlinVersion.KOTLIN_2_2

            freeCompilerArgs.addAll("-Xnested-type-aliases", "-Xcontext-sensitive-resolution", "-Xwhen-guards")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}