@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalPathApi::class)

import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.io.path.ExperimentalPathApi

plugins {
    idea
    java
    `kotlin-dsl`
    alias(libs.plugins.shadow)
    alias(libs.plugins.gradle.publishing)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kdoclink)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    api(project(":stitcher"))
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kaml)

    testImplementation(libs.bundles.test)
}

kdoclink {
    fun wiki(page: String) = "https://stonecutter.kikugie.dev/wiki/$page"

    annotation = "dev.kikugie.stonecutter.SCDocumentation"
    this["settings"] = wiki("start/settings")
    this["settings.vcs"] = wiki("start/settings#version-reset-point")
    this["settings.create"] = wiki("start/settings#specifying-versions")
    this["settings.json"] = wiki("config/params")

    this["swaps"] = wiki("config/params#string-swaps")
    this["swaps.spec"] = wiki("config/params#swap-specification")

    this["consts"] = wiki("config/params#condition-constants")
    this["consts.spec"] = wiki("config/params#constant-specification")
    this["consts.choice"] = wiki("config/params#choice-selector")

    this["deps"] = wiki("config/params#condition-dependencies")
    this["deps.spec"] = wiki("config/params#dependency-specification")

    this["utility"] = wiki("guide/setup#checking-versions")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<AbstractDokkaLeafTask>().configureEach {
    moduleName.set("Stonecutter Gradle")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.compileKotlin {
    explicitApiMode = ExplicitApiMode.Strict
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_1
        apiVersion = KotlinVersion.KOTLIN_2_1
        jvmTarget.set(JvmTarget.JVM_16)
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.shadowJar {
    archiveBaseName.set("shadow")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaJavadoc"))
}

tasks.all {
    if (this is Jar || this is DokkaTask || this is KotlinCompile)
        dependsOn(rootProject.tasks.findByName("updateVersion"))
}

tasks.withType<AbstractDokkaLeafTask> {
    moduleName = "Stonecutter Gradle"
    dokkaSourceSets.configureEach {
        samples.from("src/samples/kotlin")
    }
}

publishing {
    repositories {
        maven {
            name = "kikugieMaven"
            url = uri("https://maven.kikugie.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create("basic", BasicAuthentication::class)
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "stonecutter"
            version = project.version.toString()
            from(components["java"])
        }
    }
}

gradlePlugin {
    website = "https://stonecutter.kikugie.dev/"
    vcsUrl = "https://github.com/stonecutter-versioning/stonecutter"

    plugins {
        create("stonecutter") {
            id = "dev.kikugie.stonecutter"
            implementationClass = "dev.kikugie.stonecutter.StonecutterPlugin"
            displayName = "Stonecutter"
            description = "Modern Gradle plugin for multi-version management"
            tags = setOf("minecraft", "mods")
        }
    }
}