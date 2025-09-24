package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.util.DynamicMap
import dev.kikugie.stonecutter.build.util.PropertyBackedMap
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ProviderFactory
import kotlin.annotation.AnnotationRetention.BINARY

@DslMarker @Retention(BINARY)
private annotation class SwapDsl

@StonecutterAPI @SwapDsl
public sealed interface SwapContainer : DynamicMap<Identifier, String> {
    private class Impl(factory: ProviderFactory, property: MapProperty<Identifier, String>) :
        PropertyBackedMap<Identifier, String>(factory, property), SwapContainer {
        override fun checkKey(key: Identifier): Unit = require(key.isNotBlank() && isIdentifier(key)) { "Invalid Swap key '$key'" }
    }

    @StonecutterInternalAPI
    public companion object {
        internal operator fun invoke(factory: ProviderFactory, property: MapProperty<Identifier, String>): SwapContainer =
            Impl(factory, property)
    }
}
