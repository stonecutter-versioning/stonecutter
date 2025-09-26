import org.gradle.kotlin.dsl.register
import tasks.UpdateVersionTask

plugins {
    alias(common.plugins.kotlin.jvm) apply false
    alias(common.plugins.kotlin.serialization) apply false
    alias(common.plugins.kotlin.dokka)
}

version = property("version").toString()

repositories {
    mavenCentral()
}

dependencies {
    dokka(project(":stonecutter"))
    dokka(project(":stitcher"))

    dokkaHtmlPlugin(libs.dokka.versioning)
}

dokka {
    moduleName = "Stonecutter KDoc"

    pluginsConfiguration.html {
        homepageLink = "https://stonecutter.codeberg.page/"
        footerMessage = "(c) 2025 KikuGie"
    }

    pluginsConfiguration.versioning {
        version = property("version").toString().substringBefore('-')
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
        }
    }
}
