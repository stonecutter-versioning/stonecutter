package dev.kikugie.stonecutter.controller.ext

import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.create
import org.jetbrains.annotations.ApiStatus
import kotlin.annotation.AnnotationRetention.BINARY

@DslMarker @Retention(BINARY)
private annotation class FlagDsl

internal fun ExtensionContainer.flagContainer(name: String, flags: StonecutterFlags): MutableFlagContainer =
    create(MutableFlagContainer::class, name, Impl::class, flags)

@FlagDsl @ApiStatus.NonExtendable
public interface FlagContainer {
    public operator fun get(key: String): Any = get(StonecutterFlag.named(key))
    public operator fun <T : Any> get(key: StonecutterFlag<T>): T
    public operator fun <T : Any> StonecutterFlag<T>.invoke(): T = get(this)
}

@FlagDsl @ApiStatus.NonExtendable
public interface MutableFlagContainer : FlagContainer {
    public operator fun set(key: String, value: Any): Unit = StonecutterFlag.named(key).let {
        require(value::class == it.default::class) { "Value must be ${it.default::class.simpleName} for flag '$key'" }
        @Suppress("UNCHECKED_CAST") set(it as StonecutterFlag<Any>, value)
    }
    public operator fun <T : Any> set(key: StonecutterFlag<T>, value: T)
    public operator fun <T : Any> StonecutterFlag<T>.invoke(value: T): Unit = set(this, value)
}

private class Impl(val flags: StonecutterFlags) : MutableFlagContainer {
    override fun <T : Any> get(key: StonecutterFlag<T>): T = flags.get(key)
    override fun <T : Any> set(key: StonecutterFlag<T>, value: T) = flags.set(key, value)
}