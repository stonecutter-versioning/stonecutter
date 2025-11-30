package dev.kikugie.stonecutter.controller.file

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

public abstract class FileHandlerContainer @Inject constructor(objects: ObjectFactory) {
    internal abstract val handlers: NamedDomainObjectContainer<HandlerBuilder>

    public fun named(extension: String): NamedDomainObjectProvider<HandlerBuilder> =
        handlers.named(extension)

    public fun inherit(from: String, vararg extensions: String) {
        val parent = named(from).get()
        for (it in extensions) handlers.make(it) { copy(parent) }
    }

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