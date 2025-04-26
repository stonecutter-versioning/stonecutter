import com.github.gradle.node.npm.task.NpmTask
import org.gradle.kotlin.dsl.register
import tasks.HallOfFameTask
import tasks.UpdateVersionTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.extra.node)
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

node {
    download = true
    version = "23.11.0"
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

    register<HallOfFameTask>("updateHallOfFame") {
        group = "documentation"
        description = "Updates the Hall of Fame"

        file(".env").takeIf { it.exists() }
            ?.useLines { it.find { it.startsWith("GITHUB_TOKEN=") }?.substringAfter("=") }
            ?.let { githubToken.set(it) }

        configFile = file("docs/hof/config.yml")
        cacheFile = file("docs/hof/search.cache.yml")
        templateFile = file("docs/hof/template.md")
        outputFiles = files("docs/index.md")
    }

    register<Sync>("syncDokkaPages") {
        group = "documentation"
        from(fileTree("build/dokka/html"))
        into(file("docs/public/dokka"))
        dependsOn("dokkaGeneratePublicationHtml")
    }

    register<NpmTask>("buildDocPages") {
        group = "documentation"
        args = listOf("run", "docs:build")
        mustRunAfter("updateVersion", "updateHallOfFame", "syncDokkaPages")
    }

    register("composeDocPages") {
        group = "documentation"
        dependsOn("updateVersion", "updateHallOfFame", "syncDokkaPages", "buildDocPages")
    }
}
