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
private annotation class SwapDsl

@SwapDsl
public abstract class SwapContainer @Inject constructor(private val delegate: MapProperty<Identifier, String>) :
    ExtendedMapProperty<Identifier, String>, MapProperty<Identifier, String> by delegate {

    override fun put(key: Identifier, value: String): Unit =
        check(key) then delegate.put(key, value)

    override fun put(key: Identifier, providerOfValue: Provider<out String>): Unit =
        check(key) then delegate.put(key, providerOfValue)

    override fun putAll(entries: Map<out Identifier, String>): Unit =
        entries.keys.forEach(::check) then delegate.putAll(entries)

    override fun putAll(provider: Provider<out Map<out Identifier, String>>): Unit =
        delegate.putAll(provider.map { it.keys.forEach(::check); it })

    private fun check(key: Identifier): Unit = require(key.isValidIdentifier()) { "Invalid Stonecutter swap key '$key'" }
}