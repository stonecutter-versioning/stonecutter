package dev.kikugie.stonecutter.data.whatever

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import kotlin.reflect.KClass

/**
 * Named Gradle [Task] collection.
 *
 * @property project The associated Gradle project
 * @property type The associated task type
 */
public class TaskProviderMap<T : Task> internal constructor(
    public val project: Project,
    public val type: KClass<T>,
    internal val identifiers: MutableSet<String> = mutableSetOf(),
    internal val namer: (identifier: String) -> String,
) : Map<String, TaskProvider<T>> {
    /**
     * Set of registered task identifiers, which may not directly correspond to the task names.
     * @see tasks
     * @see taskName
     */
    override val keys: Set<String>
        get() = identifiers.toSet()
    /**List of lazily configured task providers.*/
    override val values: Collection<TaskProvider<T>>
        get() = keys.map { getOrThrow(it) }
    /**Set of [keys] to [values] pairs.*/
    override val entries: Set<Map.Entry<String, TaskProvider<T>>>
        get() = keys.map { SimpleMapEntry(it, getOrThrow(it)) }.toSet()
    /**Set of registered task names.*/
    public val tasks: Set<String>
        get() = identifiers.map(namer).toSet()

    override val size: Int get() = keys.size
    override fun isEmpty(): Boolean = keys.isEmpty()
    override fun containsKey(key: String): Boolean = key in keys
    override fun containsValue(value: TaskProvider<T>): Boolean = project.tasks.contains(value.get())

    /**Retrieves the task provider by its identifier, **not the name**.*/
    override fun get(key: String): TaskProvider<T>? = if (key in keys) getOrThrow(key) else null

    /**Returns the task name for the given identifier.*/
    public fun taskName(key: String): String = namer(key)

    /**Retrieves the task provider by its identifier, **not the name**. Throws if the task is not present.*/
    public fun getOrThrow(key: String): TaskProvider<T> = project.tasks.named(namer(key), type)

    private data class SimpleMapEntry<K : Any, V : Any>(override val key: K, override val value: V) : Map.Entry<K, V>

    internal companion object {
        inline operator fun <reified T : Task> invoke(
            project: Project,
            identifiers: MutableSet<String> = mutableSetOf(),
            noinline namer: (identifier: String) -> String
        ): TaskProviderMap<T> = TaskProviderMap(project, T::class, identifiers, namer)
    }
}