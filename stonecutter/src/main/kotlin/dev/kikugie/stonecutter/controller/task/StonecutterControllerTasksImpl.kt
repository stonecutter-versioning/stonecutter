package dev.kikugie.stonecutter.controller.task

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagsImpl
import dev.kikugie.stonecutter.controller.tree.BranchModel
import dev.kikugie.stonecutter.controller.tree.TreeModel
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.whatever.TaskProviderMap
import dev.kikugie.stonecutter.util.SCJSON
import dev.kikugie.stonecutter.util.buildDirectory
import dev.kikugie.commons.takeAs
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.util.set
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import java.io.File
import javax.inject.Inject

internal abstract class StonecutterControllerTasksImpl @Inject constructor(val controller: StonecutterControllerImpl) : StonecutterControllerTasks() {
    override val switch: TaskProviderMap<StonecutterSwitchTask> = TaskProviderMap(controller.root) {
        "stonecutterSwitchTo$it"
    }

    override fun namedImpl(name: String, cls: Class<out Task>?, filter: ProjectNode.() -> Boolean): MapProperty<ProjectNode, TaskProvider<*>> {
        val selector: TaskSelector =
            if (cls == null) TaskSelector { it.project.tasks.named(name) }
            else TaskSelector { it.project.tasks.named(name, cls) }

        val property: MapProperty<ProjectNode, TaskProvider<*>> = controller.root.objects.mapProperty()
        for (node in controller.tree.nodes.filter(filter))
            property.put(node, controller.root.provider { selector.get(node) })

        return property
    }

    override fun orderImpl(name: String, ordering: Comparator<ProjectNode>, filter: ProjectNode.() -> Boolean) {
        val lock = controller.root.buildDirectory.resolve("stonecutter-cache/$name.lock")
        val nodes = controller.tree.nodes.filter(filter).sortedWith(ordering)

        for ((a, b) in nodes.zipWithNext()) b.project.afterEvaluate {
            tasks.named(name) { outputs.file(lock); mustRunAfter((a.hierarchy + name).toString()) }
        }
    }

    fun registerScriptSwitchTask(id: Identifier, manager: StonecutterControllerManager): TaskProvider<out StonecutterSwitchTask> {
        val delegates = controller.tree.collectMergeTasks(controller.root, id)
        return controller.root.registerDefault<StonecutterScriptSwitchTask>(switch.taskName(id), { switch.identifiers += id }) {
            version.set(id)
            writer.set(manager::update)
            file.set(controller.root.buildFile)
            dependsOn(delegates)
        }
    }

    fun registerExternalSwitchTask(id: Identifier, provider: File): TaskProvider<out StonecutterSwitchTask> {
        val delegates = controller.tree.collectMergeTasks(controller.root, id)
        return controller.root.registerDefault<StonecutterExternalSwitchTask>(switch.taskName(id), { switch.identifiers += id }) {
            version.set(id)
            file.set(provider)
            dependsOn(delegates)
        }
    }

    fun registerModelGroupingTask() =
        controller.root.registerDefault<DefaultTask>("stonecutterSaveModels")

    fun registerTreeModelTask() =
        controller.root.registerDefault<StonecutterModelTask>("stonecutterSaveTreeModel") {
            val tree = controller.tree
            val flags = controller.flags.takeAs<StonecutterFlagsImpl>().storage

            output.set(controller.root.layout.buildDirectory.file("stonecutter-cache/tree.json"))
            model.set(controller.root.providers) {
                TreeModel(tree, flags).let(SCJSON::encodeToString)
            }
        }

    fun registerBranchModelTask(branch: ProjectBranch) =
        branch.project.registerDefault<StonecutterModelTask>("stonecutterSaveBranchModel") {
            output.set(branch.project.layout.buildDirectory.file("stonecutter-cache/branch.json"))
            model.set(controller.root.providers) {
                BranchModel(branch).let(SCJSON::encodeToString)
            }
        }

    private fun interface TaskSelector {
        fun get(node: ProjectNode): TaskProvider<*>
    }
}

private fun ProjectTree.collectMergeTasks(root: Project, project: Identifier) = nodes
    .filter { it.metadata.project == project }
    .map { root.provider { it.project.the<StonecutterBuildExtension>().tasks.merge.values } }

internal inline fun <reified T : Task> Project.registerDefault(
    name: String,
    consumer: (TaskProvider<T>) -> Unit = {},
    crossinline config: T.() -> Unit = {}
): TaskProvider<T> = tasks.register<T>(name) {
    group = "stonecutter-impl"
    description = "Internal Stonecutter task. Do not call manually."
    config()
}.apply(consumer)