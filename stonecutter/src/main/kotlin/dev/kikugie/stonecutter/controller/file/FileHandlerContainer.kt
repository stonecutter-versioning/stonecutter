package dev.kikugie.stonecutter.controller.file

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Extension class providing a registry for file format handlers.
 *
 * Registered handlers describe how Stonecutter should process a file of a given format.
 * File formats are associated by their lowercase extensions, without a dot prefix.
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/handlers">Wiki</a>
 */
public abstract class FileHandlerContainer @Inject constructor(objects: ObjectFactory) {
    internal abstract val handlers: NamedDomainObjectContainer<HandlerBuilder>

    /**Gets a file handler for the given extension or throws if it's not registered.*/
    public fun named(extension: String): NamedDomainObjectProvider<HandlerBuilder> =
        handlers.named(extension)

    /**Copies the configuration of the [from] extension to the provided [extensions].*/
    public fun inherit(from: String, vararg extensions: String) {
        val parent = named(from).get()
        for (it in extensions) handlers.make(it) { copy(parent) }
    }

    /**Registers or updates the configurations for the given [extensions].*/
    public fun configure(vararg extensions: String, configuration: Action<HandlerBuilder>) {
        for (it in extensions) handlers.make(it, configuration)
    }

    internal fun build(): Map<String, HandlerModel> = handlers.asMap.mapValues { (_, it) ->
        HandlerModel(it)
    }
}

private fun <T : Any> NamedDomainObjectContainer<T>.make(name: String, config: Action<T>) {
    if (name in names) named(name, config) else create(name, config)
}