package dev.kikugie.stonecutter.process

import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText

internal abstract class SCModelTask : DefaultTask(), VerificationTask {
    @get:Input
    abstract val json: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    init {
        ignoreFailures = true
    }

    @TaskAction
    fun run(): Unit = try {
        output.asFile().toPath().writeText(json(), Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    } catch (e: Exception) {
        logger.error("Failed to save model file to ${output.asFile().invariantSeparatorsPath}", e)
        throw StopExecutionException()
    }
}