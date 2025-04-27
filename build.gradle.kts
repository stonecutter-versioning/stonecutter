import tasks.UpdateVersionTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.dokka)
}

group = property("group").toString()
version = property("version").toString()

repositories {
    mavenCentral()
}

dependencies {
    dokka(project(":stonecutter"))
    dokka(project(":stitcher"))
}

dokka {
    moduleName = "Stonecutter KDoc"

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }
}

configurations.configureEach {
    if (isCanBeConsumed) attributes.attribute(
        GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
        objects.named(GradleVersion.current().version)
    )
}

tasks {
    register<UpdateVersionTask>("updateVersion") {
        group = "documentation"

        val ver = project.version.toString().removeSuffix("-SNAPSHOT")
        version = ver
        replacements {
            file("stonecutter/src/main/entrypoint/dev/kikugie/stonecutter/StonecutterPlugin.kt") replace "VERSION: String = \".+\"" with "VERSION: String = \"$ver\""
            file("docs/.vitepress/config.mts") replace "latestVersion: \".+\"" with "latestVersion: \"$ver\""
            file("docs/wiki/start/settings.md") replace listOf(
                "stonecutter\"\\ version \".+\"" to "stonecutter\" version \"$ver\"",
                "stonecutter\"\\) version \".+\"" to "stonecutter\") version \"$ver\""
            )
        }
    }
}
