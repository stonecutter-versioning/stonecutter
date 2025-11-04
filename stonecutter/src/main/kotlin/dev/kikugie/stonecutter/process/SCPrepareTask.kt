package dev.kikugie.stonecutter.process

import dev.kikugie.stitcher.issue.ProblemConsumer
import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.issue.ProblemCause
import dev.kikugie.stitcher.process
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.build.param.StonecutterBuildParameters
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import dev.kikugie.stonecutter.data.container.TaskCacheContainer
import dev.kikugie.stonecutter.util.clearIfNotIncremental
import dev.kikugie.stonecutter.util.execute
import dev.kikugie.stonecutter.util.invoke
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
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
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import javax.inject.Inject
import kotlin.io.path.*

private val REPORTED_PROBLEMS = Collections.synchronizedSet(mutableSetOf<String>())

// TODO: eventually this should use Gradle problems API, but it's still incubating
private val GRADLE_PROBLEM_REPORTER = ProblemConsumer { file: Path, location: ProblemLocation, problem: ProblemCause ->
    val problem = format(file, location, problem)
    if (REPORTED_PROBLEMS.add(problem)) System.err.println(problem)
}

private fun format(file: Path, location: ProblemLocation, problem: ProblemCause): String = buildString {
    append("e: file://${file.absolutePathString()}")
    if (location.line >= 1) {
        append(":${location.line}")
        if (location.column >= 1)
            append(":${location.column}")
    }
    append(" ${problem.message}")
    if (problem.exception != null) append("\nCaused by: ${problem.exception?.stackTraceToString()}")
}

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
    interface Parameters : WorkParameters {
        val cache: Property<TaskCacheContainer>
        val data: Property<StonecutterBuildData>
        val source: RegularFileProperty
        val output: RegularFileProperty
    }

    override fun execute() {
        val source: Path = parameters.source.asFile().toPath()
        val output: Path = parameters.output.asFile().toPath()

        val parameters = parameters.data().forFile(source, parameters.cache().handlers)
        if (!source.exists() || parameters == null) { output.deleteIfExists(); return }
        val contents = source.readText()
        val modified = process(source, contents, parameters, GRADLE_PROBLEM_REPORTER)
        if (contents == modified) output.deleteIfExists() else with(output) {
            parent.createDirectories()
            writeText(modified, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }
}