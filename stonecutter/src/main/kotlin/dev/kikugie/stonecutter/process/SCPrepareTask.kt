package dev.kikugie.stonecutter.process

import dev.kikugie.stitcher.issue.BailException
import dev.kikugie.stitcher.issue.ProblemCause
import dev.kikugie.stitcher.issue.ProblemConsumer
import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.process
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildParameters
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import dev.kikugie.stonecutter.data.container.TaskCacheContainer
import dev.kikugie.stonecutter.util.clearIfNotIncremental
import dev.kikugie.stonecutter.util.execute
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.submit
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import kotlin.io.path.*

@OptIn(StonecutterInternalAPI::class, StonecutterExperimentalFilesAPI::class)
public abstract class SCPrepareTask : DefaultTask() {
    @get:Nested
    public abstract val params: Property<StonecutterBuildParameters>

    @get:Input
    public abstract val root: Property<File>

    @get:InputFiles @get:Incremental @get:IgnoreEmptyDirectories
    public abstract val source: ConfigurableFileCollection

    @get:OutputDirectory
    public abstract val destination: DirectoryProperty

    @get:Inject
    public abstract val objects: ObjectFactory

    @get:Inject
    public abstract val executor: WorkerExecutor

    @get:ServiceReference("stonecutter-cache")
    internal abstract val cache: Property<TaskCacheContainer>

    @TaskAction
    public fun run(inputs: InputChanges) {
        inputs.clearIfNotIncremental(destination.asFile())
        val data = params().toBuildData()

        executor.execute {
            for (change in inputs.getFileChanges(source))
                if (change.fileType != FileType.DIRECTORY) it.processFile(change, data)
        }
    }

    private fun WorkQueue.processFile(change: FileChange, params: StonecutterBuildData): Unit = submit(SCPrepareAction::class) {
        data.set(params)
        source.set(change.file)
        output.set(change.file.cacheFile())
        cache.set(this@SCPrepareTask.cache)
    }

    private fun File.cacheFile(): File = destination.asFile().resolve(relativeTo(root()))
}

@OptIn(StonecutterInternalAPI::class, StonecutterExperimentalFilesAPI::class)
private interface SCPrepareAction : WorkAction<SCPrepareAction.Parameters> {
    private val source: Path get() = parameters.source.asFile().toPath()
    private val output: Path get() = parameters.output.asFile().toPath()
    private val reporter: GradleProblemReporter
        get() = GradleProblemReporter(Logging.getLogger(SCPrepareTask::class.simpleName), parameters.cache())
    private val transform: TransformParameters?
        get() = parameters.data().forFile(source, parameters.cache().handlers)

    override fun execute() {
        val transform = this.transform
        val reporter = this.reporter

        if (!source.exists() || transform == null) { output.deleteIfExists(); return }
        val contents = source.readText()
        val modified = reporter.handle { process(source, contents, transform, reporter) }
        if (contents == modified) output.deleteIfExists() else with(output) {
            parent.createDirectories()
            writeText(modified, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    interface Parameters : WorkParameters {
        val cache: Property<TaskCacheContainer>
        val data: Property<StonecutterBuildData>
        val source: RegularFileProperty
        val output: RegularFileProperty
    }
}

@OptIn(StonecutterExperimentalFilesAPI::class)
private class GradleProblemReporter(val logger: Logger, val collector: TaskCacheContainer) : ProblemConsumer {
    val errors: MutableList<String> = mutableListOf()

    inline fun <T> handle(action: () -> T) = try {
        action()
    } catch (_: BailException) {
        val formatted = errors.joinToString("\n").prependIndent("  ")
        throw GradleException("File processing failed; see errors below.\n$formatted")
    }

    override fun accept(file: Path, location: ProblemLocation, cause: ProblemCause) {
        val message = file.format(location, cause).also { errors += it }
        if (!collector.hasSeen(message)) logger.error(SC_ERROR, "e: $message", cause.exception)
    }

    private fun Path.format(location: ProblemLocation, cause: ProblemCause): String = buildString {
        append("file://${absolutePathString()}")
        if (location.line >= 1)
            append(":${location.line}")
        if (location.column >= 1)
            append(":${location.column}")
        append(": ${cause.message}")
    }

    companion object {
        val SC_ERROR: Marker = MarkerFactory.getMarker("Stonecutter")
    }
}