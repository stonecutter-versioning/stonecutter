package dev.kikugie.stonecutter.controller.tasks

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.TaskProviderMap
import dev.kikugie.stonecutter.TaskProviderMapProperty
import dev.kikugie.stonecutter.controller.StonecutterControllerImpl
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.process.SCSwitchTask
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import kotlin.reflect.KClass

/**
 * Provides structured access to tasks created by [StonecutterController][dev.kikugie.stonecutter.controller.StonecutterControllerExtension].
 */
@StonecutterAPI
public interface StonecutterControllerTasks {
    /**
     * Compares nodes by evaluating the [ProjectNode.metadata.version][dev.kikugie.stonecutter.data.StonecutterProject.version]
     * with [StonecutterControllerExtension.parse][dev.kikugie.stonecutter.controller.StonecutterControllerExtension.parse].
     */
    public val versionComparator: Comparator<ProjectNode> get() = VERSION_COMPARATOR
    /**
     * All underlying active version switch tasks.
     * Each task depends on all [StonecutterBuildTasks.merge][dev.kikugie.stonecutter.build.task.StonecutterBuildTasks.merge]
     * and updates the active version file if all succeed.
     *
     * `Set active project to ...`, `Reset active project` and `Refresh active project` tasks delegate to these.
     * You can add additional dependencies to these tasks to run when switching versions.
     */
    public val switch: TaskProviderMap<Identifier, out SCSwitchTask>

    /**Provides a switch task name for the given [StonecutterProject.project][dev.kikugie.stonecutter.data.StonecutterProject.project] name.*/
    public fun switchTaskName(project: Identifier): String = "stonecutterSwitchTo$project"

    /**Provides a switch task provider for the given [StonecutterProject.project][dev.kikugie.stonecutter.data.StonecutterProject.project] name.*/
    public fun switchTaskProvider(project: Identifier): TaskProvider<out SCSwitchTask>? =
        switch[switchTaskName(project)]

    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name] and [filter].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun named(name: String, filter: (ProjectNode.() -> Boolean)? = null): TaskProviderMapProperty<ProjectNode, *>

    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name], [class][cls] and [filter].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun <T : Task> named(name: String, cls: Class<T>, filter: (ProjectNode.() -> Boolean)? = null): TaskProviderMapProperty<ProjectNode, T>

    /**
     * Finds tasks in [ProjectTree.nodes][dev.kikugie.stonecutter.data.tree.struct.ProjectTree.nodes] matching the given [name], [class][cls] and [filter].
     * The list is live and may be empty if realised before subprojects are evaluated.
     * It can be used as-is in [Task.dependsOn], where it's final value will be used.
     */
    public fun <T : Task> named(name: String, cls: KClass<T>, filter: (ProjectNode.() -> Boolean)? = null): TaskProviderMapProperty<ProjectNode, T> =
        named(name, cls.java, filter)

    /**
     * Configures order of the provided [tasks] given the [ordering] function.
     * [Tasks][tasks] should be supplied with [stonecutter.tasks.named][named] to avoid premature realisation.
     * The [ordering] is **ascending**, meaning tasks with the greater provided value will be executed last.
     *
     * Applying this function imposes the following restrictions:
     * - You **should not** call this function multiple times for the same tasks.
     *   Doing so will result in undefined behaviour. *(but most likely a circular dependency error)*
     * - The delegated tasks must be at the end of the task graph.
     *   For example, if you apply ordering to the `build` task, but run `publish` instead,
     *   the final execution order may be different from expected.
     */
    public fun order(name: String, ordering: Comparator<ProjectNode>? = null, filter: (ProjectNode.() -> Boolean)? = null)

    public companion object {
        /**
         * Compares nodes by evaluating the [ProjectNode.metadata.version][dev.kikugie.stonecutter.data.StonecutterProject.version]
         * with [StonecutterControllerExtension.parse][dev.kikugie.stonecutter.controller.StonecutterControllerExtension.parse].
         */
        @JvmField public val VERSION_COMPARATOR: Comparator<ProjectNode> =
            Comparator.comparing { LenientOperations.parse(it.metadata.version) }

        internal fun ExtensionContainer.tasksContainer(name: String, ext: StonecutterControllerImpl): StonecutterControllerTasks =
            create(StonecutterControllerTasks::class, name, StonecutterControllerTasksImpl::class, ext)
    }
}