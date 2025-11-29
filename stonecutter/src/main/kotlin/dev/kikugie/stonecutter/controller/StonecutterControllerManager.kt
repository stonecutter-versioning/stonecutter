package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.util.overwriteText
import org.gradle.api.Project
import java.nio.file.Path
import kotlin.io.path.readText

internal interface StonecutterControllerManager {
    val filename: String
    fun create(file: Path, project: Identifier)
    fun update(file: Path, project: Identifier) {
        val original = file.readText(Charsets.UTF_8)
        val updated = original.replaceVersion(project)
        if (original != updated) file.overwriteText(updated)
    }

    companion object {
        private val PATTERN = Regex("stonecutter[\\s.]active\\s*\\(?[\"'](\\S+)[\"']\\)?")

        private fun String.replaceVersion(version: Identifier) = replace(PATTERN) { it.value.replace(it.groupValues[1], version) }

        fun Project.getController(): StonecutterControllerManager? = when (buildFile.name) {
            Groovy.filename -> Groovy
            Kotlin.filename -> Kotlin
            else -> null
        }
    }

    object Groovy : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle"

        override fun create(file: Path, project: Identifier): Unit = file.overwriteText("""
            plugins {
                id "dev.kikugie.stonecutter"
            }
            stonecutter.active "$project"
        """.trimIndent())
    }

    object Kotlin : StonecutterControllerManager {
        override val filename: String = "stonecutter.gradle.kts"
        override fun create(file: Path, project: Identifier): Unit = file.overwriteText("""
            plugins {
                id("dev.kikugie.stonecutter")
            }
            stonecutter active "$project"
        """.trimIndent())
    }
}