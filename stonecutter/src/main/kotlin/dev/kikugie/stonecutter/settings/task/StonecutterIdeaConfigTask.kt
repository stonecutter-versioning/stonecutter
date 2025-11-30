package dev.kikugie.stonecutter.settings.task

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.util.overwriteText
import dev.kikugie.stonecutter.StonecutterPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

/**Creates IntelliJ IDEA run configurations for version switch tasks.*/
public abstract class StonecutterIdeaConfigTask : DefaultTask() {
    /**Registered project trees and their nodes.*/
    @get:Input
    public abstract val projects: MapProperty<ProjectHierarchy, List<Identifier>>

    /**Run configuration files.*/
    @get:OutputFiles
    public abstract val configurations: ConfigurableFileTree

    init {
        onlyIf { TEMPLATE.isSuccess }
    }

    @TaskAction
    internal fun run() {
        val files = projects.get().entries.asSequence()
            .flatMap { (k, v) -> v.map { writeConfiguration(k, it) } }
            .toSet()

        for (file in configurations) if (file.name !in files) file
            .runCatching { delete() }
            .onFailure { logger.error("Failed to delete ${file.absolutePath}", it) }
    }

    private fun writeConfiguration(hierarchy: ProjectHierarchy, project: Identifier): String {
        val configuration = configurationFile(hierarchy, project)
        val file = configurations.dir.resolve(configuration)
        if (file.exists()) return configuration

        val xml = TEMPLATE.getOrThrow()
            .replaceChecked("%FOLDER_NAME%", "Stonecutter${hierarchy.orBlank()}")
            .replaceChecked("%ENTRY_NAME%", "Switch to $project")
            .replaceChecked("%TASK_NAME%", "${hierarchy.orBlank()}:stonecutterSwitchTo$project")
        file.overwriteText(xml)
        return configuration
    }
}

private val PATTERN: Regex = Regex("[ :]")
private val TEMPLATE: Result<String> by lazy { readResource("idea_config.xml") }

private fun readResource(path: String): Result<String> = runCatching {
    StonecutterPlugin::class.java.classLoader.getResourceAsStream(path)?.use { it.reader().readText() }
        ?: error("Resource $path not found")
}

private fun configurationFile(hierarchy: ProjectHierarchy, project: Identifier) =
    "Stonecutter${hierarchy.orBlank().formatPath()}_switchTo${project.formatPath()}.xml"

private fun String.formatPath(): String = replace(PATTERN, "_")
private fun String.replaceChecked(from: String, to: String) =
    replace(from, to.replace("\"", "&quot;"))