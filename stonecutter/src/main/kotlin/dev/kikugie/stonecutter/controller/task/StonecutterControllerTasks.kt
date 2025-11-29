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

@ControllerTasksDsl
public abstract class StonecutterControllerTasks {
    public val versionComparator: Comparator<ProjectNode> get() = VERSION_COMPARATOR

    public abstract val switch: TaskProviderMap<StonecutterSwitchTask>

    @JvmOverloads
    public fun named(name: String, filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE):  MapProperty<ProjectNode, TaskProvider<*>> =
        namedImpl(name, null, filter)

    @JvmOverloads
    public fun <T : Task> named(name: String, cls: Class<T>, filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE):  MapProperty<ProjectNode, TaskProvider<T>> =
        namedImpl(name, cls, filter).takeAs()

    @JvmOverloads
    public fun <T : Task> named(name: String, cls: KClass<T>, filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE):  MapProperty<ProjectNode, TaskProvider<T>> =
        namedImpl(name, cls.java, filter).takeAs()

    @JvmOverloads @JvmName("reifiedNamed")
    public inline fun <reified T : Task> named(name: String, noinline filter: (ProjectNode.() -> Boolean) = { true }):  MapProperty<ProjectNode, TaskProvider<T>> =
        named(name, T::class.java, filter).takeAs()

    @JvmOverloads
    public fun order(name: String, ordering: Comparator<ProjectNode> = versionComparator, filter: (ProjectNode.() -> Boolean) = ALWAYS_TRUE): Unit =
        orderImpl(name, ordering, filter)

    protected abstract fun namedImpl(name: String, cls: Class<out Task>?, filter: (ProjectNode.() -> Boolean)): MapProperty<ProjectNode, TaskProvider<*>>
    protected abstract fun orderImpl(name: String, ordering: Comparator<ProjectNode>, filter: (ProjectNode.() -> Boolean))

    private companion object {
        @JvmField val VERSION_COMPARATOR: Comparator<ProjectNode> =
            Comparator.comparing { it.metadata.parsed }
    }
}

private val ALWAYS_TRUE: (Any) -> Boolean = { true }