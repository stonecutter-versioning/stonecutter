package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.MutableTaskProviderMap
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.data.tree.model.BranchInfo
import dev.kikugie.stonecutter.data.tree.model.NodeModel
import dev.kikugie.stonecutter.process.SCPrepareTask
import dev.kikugie.stonecutter.process.SCModelTask
import dev.kikugie.stonecutter.util.*
import kotlinx.serialization.json.Json
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

@OptIn(StonecutterInternalAPI::class)
internal open class StonecutterBuildTasksImpl(private val ext: StonecutterBuildImpl) : StonecutterBuildTasks {
    override val prepare: MutableTaskProviderMap<Identifier, SCPrepareTask> = mutableMapOf()
    override val generate: MutableTaskProviderMap<Identifier, Sync> = mutableMapOf()
    override val merge: MutableTaskProviderMap<Identifier, Copy> = mutableMapOf()
    override val processedCacheDir: File get() = ext.project.buildDirectory.resolve("stonecutter-cache/sources")
    override val generatedSourcesDir: File get() = ext.project.buildDirectory.resolve("generated/stonecutter")
    private val registeredSources: MutableSet<File> = mutableSetOf()
    private val encoder = Json { prettyPrint = true }

    internal inline fun registerPrepareTask(src: SourceSet, crossinline config: SCPrepareTask.() -> Unit): TaskProvider<SCPrepareTask> =
        registerDefaultTask(prepareTaskName(src), config).apply { prepare[name] = this }

    internal inline fun registerGenerateTask(src: SourceSet, crossinline config: Sync.() -> Unit): TaskProvider<Sync> =
        registerDefaultTask(generateTaskName(src), config).apply { generate[name] = this }

    internal inline fun registerMergeTask(src: SourceSet, crossinline config: Copy.() -> Unit): TaskProvider<Copy> =
        registerDefaultTask(mergeTaskName(src), config).apply { merge[name] = this }

    internal fun registerNodeModelTask(): TaskProvider<SCModelTask> = TODO()
//        registerDefaultTask<SCModelTask>("stonecutterSaveNodeModel") {
//        output.set(ext.project.layout.buildDirectory.file("stonecutter-cache/node.json"))
//        json.set(ext.project.provider {
//            val branch = ext.branch.let { BranchInfo(it.id, it.location) }
//            NodeModel(ext.current, branch, ext.tree.location, ext.params).let(encoder::encodeToString)
//        })
//    }.also { ext.tree.project.tasks.named("stonecutterSaveModels") { dependsOn(it) } }

    private inline fun <reified T : Task> registerDefaultTask(name: String, crossinline config: T.() -> Unit): TaskProvider<T> =
        ext.project.tasks.register<T>(name) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }

    override fun configureSource(src: SourceSet) {
        val branchSrc: File = ext.project.parent!!.projectDirectory.resolve("src")
        val versionSrc: File = ext.project.projectDirectory.resolve("src")
        for (set in src.allSources()) {
            val matchingDirs = set.sourceDirectories
                .map { it.relativeTo(versionSrc) }
                .filterNot { it.startsWith("..") }

            if (ext.current.isActive) applyDirectories(set, matchingDirs, branchSrc, null, true)
            applyDirectories(set, matchingDirs, generatedSourcesDir, generateTaskName(src), !ext.current.isActive)
        }
    }

    private fun applyDirectories(set: SourceDirectorySet, matching: Iterable<File>, root: File, task: String?, apply: Boolean): List<File> {
        val dirs = matching.map(root::resolve).filterNot(registeredSources::contains).ifEmpty { return emptyList() }
        if (apply) set.srcDir(ext.project.files(dirs).apply { if (task != null) builtBy("${ext.project.path}:$task") })
        registeredSources += dirs
        return dirs
    }
}