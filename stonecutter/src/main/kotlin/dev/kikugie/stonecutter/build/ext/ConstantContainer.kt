package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.util.DynamicMap
import dev.kikugie.stonecutter.build.util.PropertyBackedMap
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ProviderFactory

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ConstantDsl

@StonecutterAPI @ConstantDsl
public sealed interface ConstantContainer : DynamicMap<Identifier, Boolean> {
    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, vararg choices: Identifier): Unit = choices.forEach { put(it, it == sample) }

    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, choices: Iterable<Identifier>): Unit = choices.forEach { put(it, it == sample) }

    private class Impl(factory: ProviderFactory, property: MapProperty<Identifier, Boolean>) :
        PropertyBackedMap<Identifier, Boolean>(factory, property), ConstantContainer {
        override fun checkKey(key: Identifier): Unit = require(key.isNotBlank() && isIdentifier(key)) { "Invalid constant key '$key'" }
    }

    @StonecutterInternalAPI
    public companion object {
        internal operator fun invoke(factory: ProviderFactory, property: MapProperty<Identifier, Boolean>): ConstantContainer =
            Impl(factory, property)
    }
}
