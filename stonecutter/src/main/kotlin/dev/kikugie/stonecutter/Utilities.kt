package dev.kikugie.stonecutter

import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.tree.BranchPrototype
import dev.kikugie.stonecutter.data.tree.NodePrototype
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import dev.kikugie.stonecutter.data.tree.TreeModel
import dev.kikugie.stonecutter.data.tree.TreePrototype
import org.gradle.api.provider.MapProperty
import kotlinx.serialization.json.Json
import org.gradle.api.tasks.SourceSetContainer
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Used as a return value by some configuration methods.
 * Generic type erasure makes `func(iter: Iterable<A>)` and `func(iter: Iterable<B>)`
 * have conflicting signatures on the JVM.
 */
public const val BNAN: String = "🍌"

// Updated by the 'updateVersion' task
/**
 * Currently running Stonecutter version, serialised in [TreeModel].
 */
public const val STONECUTTER: String = "0.6"

internal val LENIENT_JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

internal operator fun <K, V> Map<K, V>?.get(key: K): V? = this?.get(key)
internal fun <K : Any, R : Any> memoize(memory: (K) -> R?): (K) -> R? = mutableMapOf<K, R?>().let { map ->
    { key -> map.getOrPut(key) { memory(key) } }
}

internal fun String.removeStarting(char: Char): String {
    var index = 0
    while (index < length && get(index) == char) index++
    return substring(index)
}

internal val Project.sourceSets: SourceSetContainer?
    get() = project.findProperty("sourceSets") as? SourceSetContainer
internal val Project.projectPath: Path get() = projectDir.toPath()
internal val Project.buildDirectoryFile
    get() = layout.buildDirectory.asFile.get()

internal val Project.buildDirectoryPath
    get() = layout.buildDirectory.asFile.get().toPath()

internal val Project.stonecutterCacheFile
    get() = buildDirectoryFile.resolve("stonecutter-cache")

internal val Project.stonecutterCachePath
    get() = stonecutterCacheFile.toPath()

internal infix fun Path.cut(other: Path): Path = relativize(other)
internal operator fun <T> Provider<T>.invoke() = get()
internal operator fun <T> Property<T>.invoke(value: T) = set(value)

internal inline fun <T> Iterable<T>.onEach(action: T.() -> Unit) = forEach(action)
@Suppress("NOTHING_TO_INLINE")
internal inline infix fun <T> Any?.then(other: T): T = other

internal fun readResource(path: String): Result<String> = runCatching {
    StonecutterPlugin::class.java.classLoader.getResourceAsStream(path)?.use { it.reader().readText() }
        ?: error("Resource $path not found")
}

internal fun MapProperty<*, *>.keysToString() = get().keysToString()
internal fun Map<*, *>.keysToString() = keys.joinToString(prefix = "[", postfix = "]") { "'$it'" }
internal inline fun <K, V> MapProperty<K, V>.getChecked(key: K, message: Map<K, V>.(K) -> String) =
    get().getChecked(key, message)
internal inline fun <T> TreePrototype<T>.getChecked(key: ProjectHierarchy, message: TreePrototype<T>.(ProjectHierarchy) -> String) where T : BranchPrototype<out NodePrototype> =
    requireNotNull(get(key)) { message(key) }
internal inline fun <T> BranchPrototype<T>.getChecked(key: ProjectHierarchy, message: BranchPrototype<T>.(ProjectHierarchy) -> String) where T : NodePrototype =
    requireNotNull(get(key)) { message(key) }
internal inline fun <K, V> Map<K, V>.getChecked(
    key: K,
    message: Map<K, V>.(K) -> String = { "Key '$key' not found in ${keysToString()}" }
): V = requireNotNull(get(key)) { message(key) }
internal inline fun <reified T> requireAs(value: Any?, message: (KClass<*>?) -> String): T = when (value) {
    is T -> value
    null -> throw kotlin.IllegalArgumentException(message(null))
    else -> throw IllegalArgumentException(message(T::class))
}