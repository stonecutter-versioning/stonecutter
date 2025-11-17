import org.gradle.kotlin.dsl.register
import tasks.UpdateVersionTask

plugins {
    alias(common.plugins.kotlin.jvm) apply false
    alias(common.plugins.kotlin.serialization) apply false
    alias(common.plugins.kotlin.dokka)
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
