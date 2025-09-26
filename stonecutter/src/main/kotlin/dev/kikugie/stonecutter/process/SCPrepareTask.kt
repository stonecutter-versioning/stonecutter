package dev.kikugie.stonecutter.process

import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.issue.ProblemReporter
import dev.kikugie.stitcher.issue.ProblemTemplate
import dev.kikugie.stitcher.process
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildParameters
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
import java.util.Collections
import javax.inject.Inject
import kotlin.io.path.*

private val REPORTED_PROBLEMS = Collections.synchronizedSet(mutableSetOf<String>())

// TODO: eventually this should use Gradle problems API, but it's still incubating
private val GRADLE_PROBLEM_REPORTER = ProblemReporter { file, location, template ->
    val problem = format(file, location, template)
    if (REPORTED_PROBLEMS.add(problem)) System.err.println(problem)
}

private fun format(file: Path, location: ProblemLocation, template: ProblemTemplate): String = buildString {
    append("e: file://${file.absolutePathString()}")
    if (location.line >= 1) {
        append(":${location.line}")
        if (location.offset >= 0)
            append(":${location.offset}")
    }
    append(" ${template.message}")
    if (template.cause != null) append("\nCaused by: ${template.cause?.stackTraceToString()}")
}

@OptIn(StonecutterInternalAPI::class)
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

    @TaskAction
    public fun run(inputs: InputChanges) {
        inputs.clearIfNotIncremental(destination.asFile())
        val data = params().toTransformParameters()

        executor.execute {
            for (change in inputs.getFileChanges(source))
                if (change.fileType != FileType.DIRECTORY) it.processFile(change, data)
        }
    }

    private fun WorkQueue.processFile(change: FileChange, data: TransformParameters): Unit = submit(SCPrepareAction::class) {
        params.set(data)
        source.set(change.file)
        output.set(change.file.cacheFile())
    }

    private fun File.cacheFile(): File = destination.asFile().resolve(relativeTo(root()))
}

@OptIn(StonecutterInternalAPI::class)
private interface SCPrepareAction : WorkAction<SCPrepareAction.Parameters> {
    interface Parameters : WorkParameters {
        val params: Property<TransformParameters>
        val source: RegularFileProperty
        val output: RegularFileProperty
    }

    override fun execute() {
        val source: Path = parameters.source.asFile().toPath()
        val output: Path = parameters.output.asFile().toPath()

        if (!source.exists()) { output.deleteIfExists(); return }
        val contents = source.readText()
        val modified = process(source, contents, parameters.params(), GRADLE_PROBLEM_REPORTER)
        if (contents == modified) output.deleteIfExists() else with(output) {
            parent.createDirectories()
            writeText(modified, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }
}