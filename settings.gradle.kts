pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.kikugie.dev/releases")
    }

    versionCatalogs {
        create("common") { from("dev.kikugie:stonecutter-versions:1.4.0") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "Stonecutter"
include("stitcher", "stonecutter")