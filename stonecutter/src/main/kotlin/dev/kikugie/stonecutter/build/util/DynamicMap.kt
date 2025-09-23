package dev.kikugie.stonecutter.build.util

import org.gradle.api.provider.Provider

/**
 * A [MutableMap] that accepts [Supplier<E>][java.util.function.Supplier] and [Provider<E>][org.gradle.api.provider.Provider] values.
 * Dynamic values are not checked until the map is queried, which may have duplicates until the realised map is built.
 * Bulk additions don't support dynamic suppliers due to type erasure.
 */
public interface DynamicMap<K : Any, V : Any> : MutableMap<K, V> {
    public operator fun set(key: K, value: V) {
        put(key, value)
    }

    public fun put(key: K, supplier: () -> V): V?
    public operator fun set(key: K, supplier: () -> V) {
        put(key, supplier)
    }

    public fun put(key: K, provider: Provider<V>): V?
    public operator fun set(key: K, provider: Provider<V>) {
        put(key, provider)
    }
}