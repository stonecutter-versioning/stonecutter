package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.util.overwriteText
import org.gradle.api.Project
import java.nio.file.Path
import kotlin.io.path.readText

internal interface StonecutterControllerManager {
    val filename: String
    val pattern: Regex

    fun create(file: Path, project: Identifier)
    fun update(file: Path, project: Identifier) {
        val original = file.readText(Charsets.UTF_8)
        val updated = replace(original, project)
        if (original != updated) file.overwriteText(updated)
    }

    fun replace(text: String, template: String): String

    companion object {
        fun Project.getController(): StonecutterControllerManager? = when (buildFile.name) {
            Groovy.filename -> Groovy
            Kotlin.filename -> Kotlin
            else -> null
        }
    }

    object Groovy : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle"
        override val pattern: Regex = Regex("""(sc|stonecutter)\s*\.\s*active\s*\(?\s*["'](\S+)["']\s*\)?""")

        override fun create(file: Path, project: Identifier): Unit = file.overwriteText("""
            plugins {
                id "dev.kikugie.stonecutter"
            }
            stonecutter.active "$project"
        """.trimIndent())

        override fun replace(text: String, template: String): String = text.replace(pattern) {
            it.value.replace(it.groupValues[2], template)
        }
    }

    object Kotlin : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle.kts"
        override val pattern: Regex = Regex("""(sc|stonecutter)\s*\.?\s*active\s*\(?\s*"(\S+)"\s*\)?""")

        override fun create(file: Path, project: Identifier): Unit = file.overwriteText("""
            plugins {
                id("dev.kikugie.stonecutter")
            }
            stonecutter active "$project"
        """.trimIndent())

        override fun replace(text: String, template: String): String = text.replace(pattern) {
            it.value.replace(it.groupValues[2], template)
        }
    }
}