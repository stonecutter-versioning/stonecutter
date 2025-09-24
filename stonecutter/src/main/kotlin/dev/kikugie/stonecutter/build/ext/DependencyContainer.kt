package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.build.util.DynamicMap
import dev.kikugie.stonecutter.build.util.PropertyBackedMap
import dev.kikugie.stonecutter.util.isIdentifier
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ProviderFactory

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class DependencyDsl

@StonecutterAPI @DependencyDsl
public sealed interface DependencyContainer : DynamicMap<Identifier, Version> {
    private open class Impl(factory: ProviderFactory, property: MapProperty<Identifier, Version>) :
        PropertyBackedMap<Identifier, Version>(factory, property), DependencyContainer {
        override fun checkKey(key: Identifier): Unit = require(key.isNotBlank() && isIdentifier(key)) { "Invalid Dependency key '$key'" }
    }

    @StonecutterInternalAPI
    public companion object {
        internal operator fun invoke(factory: ProviderFactory, property: MapProperty<Identifier, Version>): DependencyContainer =
            Impl(factory, property)
    }
}
