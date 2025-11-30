package dev.kikugie.stonecutter.build.config

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.whatever.ExtendedMapProperty
import dev.kikugie.commons.then
import dev.kikugie.stitcher.util.isValidIdentifier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.mapProperty
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ConstantDsl

/**[Stonecutter constant](https://stonecutter.kikugie.dev/wiki/config/params#condition-constants) configuration extension.*/
@ConstantDsl
public abstract class ConstantContainer @Inject constructor(private val delegate: MapProperty<Identifier, Boolean>) :
    ExtendedMapProperty<Identifier, Boolean>, MapProperty<Identifier, Boolean> by delegate {
    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, vararg choices: Identifier): Unit = choices.forEach { put(it, it == sample) }

    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, choices: Iterable<Identifier>): Unit = choices.forEach { put(it, it == sample) }

    override fun put(key: Identifier, value: Boolean): Unit =
        check(key) then delegate.put(key, value)

    override fun put(key: Identifier, providerOfValue: Provider<out Boolean>): Unit =
        check(key) then delegate.put(key, providerOfValue)

    override fun putAll(entries: Map<out Identifier, Boolean>): Unit =
        entries.keys.forEach(::check) then delegate.putAll(entries)

    override fun putAll(provider: Provider<out Map<out Identifier, Boolean>>): Unit =
        delegate.putAll(provider.map { it.keys.forEach(::check); it })

    private fun check(key: Identifier): Unit = require(key.isValidIdentifier()) { "Invalid Stonecutter constant key '$key'" }
}
