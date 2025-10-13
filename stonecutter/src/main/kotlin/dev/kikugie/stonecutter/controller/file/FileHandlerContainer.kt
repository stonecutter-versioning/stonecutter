package dev.kikugie.stonecutter.controller.file

import dev.kikugie.commons.collections.present
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class FileContainerDsl

@StonecutterExperimentalFilesAPI @FileContainerDsl
public abstract class FileHandlerContainer : java.io.Serializable {
    public val handlers: MutableMap<String, FileHandlerBuilder> = HashMap()
    @get:Inject
    protected abstract val objects: ObjectFactory

    internal operator fun get(extension: String): FileHandlerBuilder? = handlers[extension]

    public fun configure(vararg formats: String, action: FileHandlerBuilder.() -> Unit): Unit = synchronized(handlers) {
        for (ext in formats) handlers.compute(ext) { e, it ->
            (it ?: objects.newInstance<FileHandlerBuilder>(e)).apply(action)
        }
    }

    public fun configureIfPresent(vararg formats: String, action: FileHandlerBuilder.() -> Unit): Unit = synchronized(handlers) {
        for (ext in formats) handlers.computeIfPresent(ext) { _, it ->
            it.apply(action)
        }
    }

    public fun configureIfAbsent(vararg formats: String, action: FileHandlerBuilder.() -> Unit): Unit = synchronized(handlers) {
        for (ext in formats) handlers.computeIfAbsent(ext) { e ->
            objects.newInstance<FileHandlerBuilder>(e).apply(action)
        }
    }
}