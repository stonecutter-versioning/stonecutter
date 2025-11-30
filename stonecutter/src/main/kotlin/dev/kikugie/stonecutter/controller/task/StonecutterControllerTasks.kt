package dev.kikugie.stonecutter.controller.task

import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.whatever.TaskProviderMap
import dev.kikugie.commons.takeAs
import org.gradle.api.Task
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.TaskProvider
import kotlin.reflect.KClass

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ControllerTasksDsl

/**
 * Extension class providing access to [StonecutterControllerExtension][dev.kikugie.stonecutter.controller.StonecutterControllerExtension]
 * tasks and aggregation utilities.
 */
@ControllerTasksDsl
public abstract class StonecutterControllerTasks {
    /**The default node ordering used in [order], sorting by [StonecutterProject.parsed][dev.kikugie.stonecutter.data.StonecutterProject.parsed].*/
    public val versionComparator: Comparator<ProjectNode> get() = VERSION_COMPARATOR

    /**All `stonecutterSwitchTo..` tasks.*/
    public abstract val switch: TaskProviderMap<StonecutterSwitchTask>

    /**Returns a lazily evaluated collection of tasks in nodes with the given [name].*/
    @JvmOverloads
    public fun named(
        name: String,
        filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE
    ): MapProperty<ProjectNode, TaskProvider<*>> =
        namedImpl(name, null, filter)

    /**Returns a lazily evaluated collection of tasks in nodes with the given [name] and [cls].*/
    @JvmOverloads
    public fun <T : Task> named(
        name: String,
        cls: Class<T>,
        filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE
    ): MapProperty<ProjectNode, TaskProvider<T>> =
        namedImpl(name, cls, filter).takeAs()

    /**Returns a lazily evaluated collection of tasks in nodes with the given [name] and [cls].*/
    @JvmOverloads
    public fun <T : Task> named(
        name: String,
        cls: KClass<T>,
        filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE
    ): MapProperty<ProjectNode, TaskProvider<T>> =
        namedImpl(name, cls.java, filter).takeAs()

    /**Returns a lazily evaluated collection of tasks in nodes with the given [name] and [T] class.*/
    @JvmOverloads @JvmName("reifiedNamed")
    public inline fun <reified T : Task> named(
        name: String,
        noinline filter: (ProjectNode.() -> Boolean) = { true }
    ): MapProperty<ProjectNode, TaskProvider<T>> =
        named(name, T::class.java, filter).takeAs()

    /**
     * Forces the tasks in nodes with the given [name] to execute with the given [ordering].
     *
     * Important considerations:
     * - This function must be called at most once per task name.
     * Subsequent calls may result in undefined behaviour or a cyclical dependency.
     * - The targeted task must be an endpoint task, otherwise the function will have no effect.
     * For example, `publishMods` is not an endpoint task as it just calls `publishModrinth`
     * and `publishCurseforge`, which should be ordered instead.
     */
    @JvmOverloads
    public fun order(
        name: String,
        ordering: Comparator<ProjectNode> = versionComparator,
        filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE
    ): Unit =
        orderImpl(name, ordering, filter)

    protected abstract fun namedImpl(
        name: String,
        cls: Class<out Task>?,
        filter: (ProjectNode.() -> Boolean)
    ): MapProperty<ProjectNode, TaskProvider<*>>

    protected abstract fun orderImpl(name: String, ordering: Comparator<ProjectNode>, filter: (ProjectNode.() -> Boolean))

    private companion object {
        /**The default node ordering used in [order], sorting by [StonecutterProject.parsed][dev.kikugie.stonecutter.data.StonecutterProject.parsed].*/
        @JvmField val VERSION_COMPARATOR: Comparator<ProjectNode> =
            Comparator.comparing { it.metadata.parsed }
    }
}

private val ALWAYS_TRUE: (Any) -> Boolean = { true }