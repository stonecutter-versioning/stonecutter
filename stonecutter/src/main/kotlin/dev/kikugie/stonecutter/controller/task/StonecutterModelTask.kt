package dev.kikugie.stonecutter.controller.task

import dev.kikugie.stonecutter.util.overwriteText
import dev.kikugie.stonecutter.util.toPath
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import kotlin.io.path.absolutePathString

/**
 * Saves [ProjectTree][dev.kikugie.stonecutter.data.tree.ProjectTree],
 * [ProjectBranch][dev.kikugie.stonecutter.data.tree.ProjectBranch] and
 * [ProjectNode][dev.kikugie.stonecutter.data.tree.ProjectNode]
 * data to `build/stonecutter-cache/<>.json` for third-party access.
 */
public abstract class StonecutterModelTask : DefaultTask() {
    /**The saved JSON data.*/
    @get:Input
    public abstract val model: Property<String>

    /**The save location.*/
    @get:OutputFile
    public abstract val output: RegularFileProperty

    @TaskAction
    internal fun run(): Unit = try {
        output.toPath().overwriteText(model.get())
    } catch (e: IOException) {
        logger.error("Failed to save model file to ${output.toPath().absolutePathString()}", e)
        throw StopExecutionException()
    }
}