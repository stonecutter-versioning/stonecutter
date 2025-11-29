package dev.kikugie.stonecutter.data.whatever

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider

public interface ExtendedMapProperty<K : Any, V : Any> : MapProperty<K, V> {
    public operator fun set(key: K, value: V) {
        put(key, value)
    }

    public operator fun set(key: K, provider: Provider<V>) {
        put(key, provider)
    }
}