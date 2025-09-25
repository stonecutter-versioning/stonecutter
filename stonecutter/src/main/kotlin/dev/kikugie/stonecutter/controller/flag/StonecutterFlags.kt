package dev.kikugie.stonecutter.controller.flag

@Suppress("UNCHECKED_CAST")
internal class StonecutterFlags(
    private val map: MutableMap<String, Any> = mutableMapOf(),
    private var properties: (StonecutterFlag<*>) -> String? = { null }
) {
    val serializable: Map<String, String>
        get() = map.mapValues { (_, it) -> it.toString() }

    operator fun <T : Any> get(key: StonecutterFlag<T>): T {
        val result = map[key.key] ?: properties(key)?.let(key::fromString) ?: key.default
        return result as T
    }

    operator fun <T : Any> set(key: StonecutterFlag<T>, value: T) {
        map[key.key] = value
    }
}