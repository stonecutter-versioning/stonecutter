pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }

    versionCatalogs {
        create("common") { from("dev.kikugie:stonecutter-versions:1-SNAPSHOT") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "Stonecutter"
include("stonecutter", "stitcher", "stitcher_antlr")