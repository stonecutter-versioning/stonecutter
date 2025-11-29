package dev.kikugie.stonecutter.data.whatever

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import kotlin.reflect.KClass

public class TaskProviderMap<T : Task> internal constructor(
    public val project: Project,
    public val type: KClass<T>,
    internal val identifiers: MutableSet<String> = mutableSetOf(),
    internal val namer: (identifier: String) -> String,
) : Map<String, TaskProvider<T>> {
    override val values: Collection<TaskProvider<T>>
        get() = keys.map { getOrThrow(it) }
    override val entries: Set<Map.Entry<String, TaskProvider<T>>>
        get() = keys.map { SimpleMapEntry(it, getOrThrow(it)) }.toSet()
    override val keys: Set<String>
        get() = identifiers.toSet()
    public val tasks: Set<String>
        get() = identifiers.map(namer).toSet()

    override val size: Int get() = keys.size
    override fun isEmpty(): Boolean = keys.isEmpty()
    override fun containsKey(key: String): Boolean = key in keys
    override fun containsValue(value: TaskProvider<T>): Boolean = project.tasks.contains(value.get())
    override fun get(key: String): TaskProvider<T>? = if (key in keys) getOrThrow(key) else null

    public fun taskName(key: String): String = namer(key)
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