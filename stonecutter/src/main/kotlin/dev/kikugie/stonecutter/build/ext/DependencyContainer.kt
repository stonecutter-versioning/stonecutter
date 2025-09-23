package dev.kikugie.stonecutter.build.ext

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.Version
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
private annotation class DependencyDsl

@DependencyDsl @ApiStatus.NonExtendable
public interface DependencyContainer : DynamicMap<Identifier, Version> {
    private open class Impl @Inject constructor(property: MapProperty<Identifier, Version>, factory: ProviderFactory) :
        PropertyBackedMap<Identifier, Version>(factory, property), DependencyContainer {
        override fun checkKey(key: Identifier) = require(key.isNotBlank() && isIdentifier(key)) { "Invalid Dependency key '$key'" }
    }

    @StonecutterInternalAPI
    public companion object {
        internal fun ExtensionContainer.dependencyContainer(name: Version, property: MapProperty<Identifier, Version>): DependencyContainer =
            create(DependencyContainer::class, name, Impl::class, property)
    }
}
