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
private annotation class SwapDsl

@SwapDsl @ApiStatus.NonExtendable
public interface SwapContainer : DynamicMap<Identifier, String> {
    private open class Impl @Inject constructor(property: MapProperty<Identifier, String>, factory: ProviderFactory) :
        PropertyBackedMap<Identifier, String>(factory, property), SwapContainer {
        override fun checkKey(key: Identifier) = require(key.isNotBlank() && isIdentifier(key)) { "Invalid Swap key '$key'" }
    }

    @StonecutterInternalAPI
    public companion object {
        internal fun ExtensionContainer.swapContainer(name: String, property: MapProperty<Identifier, String>): SwapContainer =
            create(SwapContainer::class, name, Impl::class, property)
    }
}
