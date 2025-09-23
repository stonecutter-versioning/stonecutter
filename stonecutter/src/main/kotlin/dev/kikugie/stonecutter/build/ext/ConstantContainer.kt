package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.util.DynamicMap
import dev.kikugie.stonecutter.build.util.PropertyBackedMap
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.create
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject

@DslMarker @Retention(BINARY)
private annotation class ConstantDsl

@ConstantDsl @ApiStatus.NonExtendable
public interface ConstantContainer : DynamicMap<Identifier, Boolean> {
    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, vararg choices: Identifier): Unit = choices.forEach { put(it, it == sample) }

    /**Puts all [choices] into the constant map, with their values set to `true` if they equal [sample].*/
    public fun match(sample: Identifier, choices: Iterable<Identifier>): Unit = choices.forEach { put(it, it == sample) }

    private open class Impl @Inject constructor(property: MapProperty<Identifier, Boolean>, factory: ProviderFactory) :
        PropertyBackedMap<Identifier, Boolean>(factory, property), ConstantContainer {
        override fun checkKey(key: Identifier) = require(key.isNotBlank() && isIdentifier(key)) { "Invalid constant key '$key'" }
    }

    @StonecutterInternalAPI
    public companion object {
        internal fun ExtensionContainer.constantContainer(name: String, property: MapProperty<Identifier, Boolean>): ConstantContainer =
            create(ConstantContainer::class, name, Impl::class, property)
    }
}
