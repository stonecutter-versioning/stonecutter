package dev.kikugie.stonecutter.controller.flag

import kotlin.annotation.AnnotationRetention.BINARY

@DslMarker @Retention(BINARY)
private annotation class FlagDsl

@FlagDsl
public sealed interface StonecutterFlagsView {
    public operator fun get(key: String): Any = get(StonecutterFlag.named(key))
    public operator fun <T : Any> get(key: StonecutterFlag<T>): T
    public operator fun <T : Any> StonecutterFlag<T>.invoke(): T = get(this)
}

@FlagDsl
public sealed interface StonecutterFlags : StonecutterFlagsView {
    public operator fun set(key: String, value: Any): Unit = StonecutterFlag.named(key).let {
        require(value::class == it.default::class) { "Value must be ${it.default::class.simpleName} for flag '$key'" }
        @Suppress("UNCHECKED_CAST") set(it as StonecutterFlag<Any>, value)
    }
    public operator fun <T : Any> set(key: StonecutterFlag<T>, value: T)
    public operator fun <T : Any> StonecutterFlag<T>.invoke(value: T): Unit = set(this, value)
}

internal class StonecutterFlagsImpl(val storage: StonecutterFlagStorage) : StonecutterFlags {
    override fun <T : Any> get(key: StonecutterFlag<T>): T = storage.get(key)
    override fun <T : Any> set(key: StonecutterFlag<T>, value: T) = storage.set(key, value)
}