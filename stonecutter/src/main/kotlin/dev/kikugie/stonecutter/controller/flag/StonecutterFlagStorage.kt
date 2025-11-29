package dev.kikugie.stonecutter.controller.flag

/**Stores [StonecutterFlag]s with access to project properties and defaults.*/
@Suppress("UNCHECKED_CAST")
internal class StonecutterFlagStorage(
    val values: MutableMap<String, Any> = mutableMapOf(),
    val provider: (StonecutterFlag<*>) -> String? = { null }
) {
    val serializable: Map<String, String>
        get() = values.mapValues { (_, it) -> it.toString() }

    operator fun <T : Any> get(key: StonecutterFlag<T>): T {
        val result = values[key.key] ?: provider(key)?.let(key::fromString) ?: key.default
        return result as T
    }

    operator fun <T : Any> set(key: StonecutterFlag<T>, value: T) {
        values[key.key] = value
    }
}