package dev.kikugie.stonecutter.build.config

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.whatever.ExtendedMapProperty
import dev.kikugie.commons.then
import dev.kikugie.stitcher.util.isValidIdentifier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.mapProperty
import javax.inject.Inject
import kotlin.collections.forEach

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class DependencyDsl

@DependencyDsl
public abstract class DependencyContainer @Inject constructor(private val delegate: MapProperty<Identifier, AnyVersion>) :
    ExtendedMapProperty<Identifier, AnyVersion>, MapProperty<Identifier, AnyVersion> by delegate {
    override fun put(key: Identifier, value: AnyVersion): Unit =
        check(key) then delegate.put(key, value)

    override fun put(key: Identifier, providerOfValue: Provider<out AnyVersion>): Unit =
        check(key) then delegate.put(key, providerOfValue)

    override fun putAll(entries: Map<out Identifier, AnyVersion>): Unit =
        entries.keys.forEach(::check) then delegate.putAll(entries)

    override fun putAll(provider: Provider<out Map<out Identifier, AnyVersion>>): Unit =
        delegate.putAll(provider.map { it.keys.forEach(::check); it })

    private fun check(key: Identifier): Unit = require(key.isValidIdentifier()) { "Invalid Stonecutter dependency key '$key'" }
}