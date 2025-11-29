package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.controller.task.StonecutterModelTask
import dev.kikugie.stonecutter.controller.task.registerDefault
import dev.kikugie.stonecutter.controller.tree.NodeModel
import dev.kikugie.stonecutter.data.whatever.TaskProviderMap
import dev.kikugie.stonecutter.util.SCJSON
import dev.kikugie.stonecutter.util.allSources
import dev.kikugie.stonecutter.util.buildDirectory
import dev.kikugie.stonecutter.util.set
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import java.io.File
import javax.inject.Inject

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildTasksImpl @Inject constructor(val build: StonecutterBuildImpl) : StonecutterBuildTasks {
    override val prepare: TaskProviderMap<StonecutterPrepareTask> = TaskProviderMap(build.project) {
        "stonecutterPrepare${it.sourceSetSuffix}"
    }
    override val generate: TaskProviderMap<Sync> = TaskProviderMap(build.project) {
        "stonecutterGenerate${it.sourceSetSuffix}"
    }
    override val merge: TaskProviderMap<Copy> = TaskProviderMap(build.project) {
        "stonecutterMerge${it.sourceSetSuffix}"
    }

    private val registeredSources: MutableSet<File> = mutableSetOf()
    private val logger: Logger = Logging.getLogger("StonecutterBuild")

    init {
        val buildDir = build.project.layout.buildDirectory
        processedCacheDir.value(buildDir.dir("stonecutter-cache/sources")).finalizeValue()
        generatedSourcesDir.value(buildDir.dir("generated/stonecutter")).finalizeValue()
    }

    override fun configureSource(src: SourceSet) {
        val branchSrc: File = build.branch.location.resolve("src").toFile()
        val versionSrc: File = build.node.location.resolve("src").toFile()
        val generatedSrc: File = generatedSourcesDir.asFile.get()
        val taskName: String = generate.taskName(src.name)

        for (entry in src.allSources()) {
            val matching = entry.sourceDirectories
                .map { it.relativeTo(versionSrc) }
                .filterNot { it.startsWith("..") }

            if (build.current.isActive) entry.extend(matching, branchSrc, null, true)
            entry.extend(matching, generatedSrc, taskName, !build.current.isActive)
        }
    }

    inline fun registerPrepareTask(src: SourceSet, crossinline config: StonecutterPrepareTask.() -> Unit): TaskProvider<StonecutterPrepareTask> =
        build.project.registerDefault(prepare.taskName(src.name), { prepare.identifiers += src.name }, config)

    inline fun registerGenerateTask(src: SourceSet, crossinline config: Sync.() -> Unit): TaskProvider<Sync> =
        build.project.registerDefault(generate.taskName(src.name), { generate.identifiers += src.name }, config)

    inline fun registerMergeTask(src: SourceSet, crossinline config: Copy.() -> Unit): TaskProvider<Copy> =
        build.project.registerDefault(merge.taskName(src.name), { merge.identifiers += src.name }, config)

    fun registerNodeModelTask(): TaskProvider<StonecutterModelTask> = build.project.registerDefault(
        "stonecutterSaveNodeModel",
        { build.tree.project.afterEvaluate { tasks.named("stonecutterSaveModels") { dependsOn(it) } } }
    ) {
        val node = build.node

        output.set(build.project.layout.buildDirectory.file("stonecutter-cache/node.json"))
        model.set(build.project.providers) {
            NodeModel(node, build.config.data).let(SCJSON::encodeToString)
        }
    }

    private fun SourceDirectorySet.extend(matching: Iterable<File>, root: File, task: String?, apply: Boolean) {
        val filtered = matching.map(root::resolve).filterNot(registeredSources::contains)
            .ifEmpty { return }

        if (apply) {
            val collection = build.project.files(filtered)
            if (task != null) collection.builtBy("${build.node.hierarchy}:$task")
            srcDir(collection)
        }

        registeredSources += filtered
        if (logger.isDebugEnabled) for (dir in filtered) logger.debug(buildString {
            append("Registered source directory ${dir.absolutePath}")
            if (task != null) append(" << $task")
        })
    }
}

private val String.sourceSetSuffix: String
    get() = if (this == "main") "" else replaceFirstChar(Char::uppercase)