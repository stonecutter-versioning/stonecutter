package dev.kikugie.stonecutter.process

import dev.kikugie.commons.collections.present
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
import javax.inject.Inject
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

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
        val data = params.get().build()

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
        // FIXME: This does nothing so far
    }
}