package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.commons.takeAs
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.MutableTaskProviderMap
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.TaskProviderMapProperty
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.process.SCExternalSwitchTask
import dev.kikugie.stonecutter.process.SCScriptSwitchTask
import dev.kikugie.stonecutter.process.SCSwitchTask
import dev.kikugie.stonecutter.util.buildDirectory
import dev.kikugie.stonecutter.util.invoke
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import java.io.File

private val TRUE: (Any) -> Boolean = { true }

@OptIn(StonecutterInternalAPI::class)
internal open class StonecutterControllerTasksImpl(val ext: StonecutterControllerImpl) : StonecutterControllerTasks {
    override val switch: MutableTaskProviderMap<Identifier, out SCSwitchTask> = mutableMapOf()
    private val encoder = Json { prettyPrint = true }
    override fun named(name: String, filter: (ProjectNode.() -> Boolean)?): TaskProviderMapProperty<ProjectNode, *> =
        named(filter ?: TRUE) { it.project.tasks.named(name) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Task> named(name: String, cls: Class<T>, filter: (ProjectNode.() -> Boolean)?): TaskProviderMapProperty<ProjectNode, T> =
        named(filter ?: TRUE) { it.project.tasks.named(name, cls) } as TaskProviderMapProperty<ProjectNode, T>

    private inline fun named(
        crossinline filter: ProjectNode.() -> Boolean,
        crossinline selector: (ProjectNode) -> TaskProvider<*>
    ): TaskProviderMapProperty<ProjectNode, *> = ext.root.objects.mapProperty<ProjectNode, TaskProvider<*>>()
        .value(ext.root.provider { ext.tree.nodes.filter(filter).associateWith(selector) })

    override fun order(name: String, ordering: Comparator<ProjectNode>?, filter: (ProjectNode.() -> Boolean)?) {
        val lock = ext.root.buildDirectory.resolve("stonecutter-cache/sc.lock")
        val nodes = ext.tree.nodes.filter(filter ?: TRUE).sortedWith(ordering ?: StonecutterControllerTasks.VERSION_COMPARATOR)

        for ((a, b) in nodes.zipWithNext()) {
            val prev = "${a.hierarchy.orBlank()}:$name"
            b.project.afterEvaluate {
                tasks.named(name) { outputs.file(lock); mustRunAfter(prev) }
            }
        }
    }

    fun registerSelfSwitchTask(project: Identifier, manager: StonecutterControllerManager): TaskProvider<SCScriptSwitchTask> {
        val merges = getMergeTasks(project)
        return ext.root.registerDefaultTask<SCScriptSwitchTask>(switchTaskName(project)) {
            manager(manager::class.java)
            version(project)
            script.set(ext.root.buildFile)
            dependsOn(merges)
        }.apply { switch[name] = this }
    }

    fun registerExternalSwitchTask(project: Identifier, provider: File): TaskProvider<SCExternalSwitchTask> {
        val merges = getMergeTasks(project)
        return ext.root.registerDefaultTask<SCExternalSwitchTask>(switchTaskName(project)) {
            version(project)
            file.set(provider)
            dependsOn(merges)
        }.apply { switch[name] = this }
    }

    fun registerModelGroupingTask() = ext.root.registerDefaultTask<DefaultTask>("stonecutterSaveModels")

//    fun registerTreeModelTask() = ext.root.registerDefaultTask<SCModelTask>("stonecutterSaveTreeModel") {
//        output.set(ext.root.layout.buildDirectory.file("stonecutter-cache/tree.json"))
//        json.set(ext.root.provider {
//            val branches = ext.tree.branches.map { BranchInfo(it.id, it.location) }
//            val nodes = ext.tree.nodes.map { NodeInfo(it.metadata, it.location) }
//            TreeModel(StonecutterPlugin.VERSION, ext.tree.vcs.project, ext.tree.current?.project, branches, nodes, ext.flags, ext.activeInfo)
//                .let(encoder::encodeToString)
//        })
//    }.also {
//        ext.root.tasks.named("stonecutterSaveModels") { dependsOn(it) }
//    }
//
//    fun registerBranchModelTask(branch: ProjectBranch) = branch.project.registerDefaultTask<SCModelTask>("stonecutterSaveBranchModel") {
//        output.set(branch.project.layout.buildDirectory.file("stonecutter-cache/branch.json"))
//        json.set(ext.root.provider {
//            val nodes = branch.nodes.map { NodeInfo(it.metadata, it.location) }
//            BranchModel(branch.id, ext.tree.location, nodes).let(encoder::encodeToString)
//        })
//    }.also {
//        ext.root.tasks.named("stonecutterSaveModels") { dependsOn(it) }
//    }

    private inline fun <reified T : Task> Project.registerDefaultTask(name: String, crossinline config: T.() -> Unit = {}): TaskProvider<T> =
        tasks.register<T>(name) {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."
            config()
        }

    private fun getMergeTasks(project: Identifier) = ext.tree.nodes
        .filter { it.metadata.project == project }
        .map {
            ext.root.provider {
                it.project.the<StonecutterBuildExtension>().takeAs<StonecutterBuildImpl>().tasks.merge.values
            }
        }
}