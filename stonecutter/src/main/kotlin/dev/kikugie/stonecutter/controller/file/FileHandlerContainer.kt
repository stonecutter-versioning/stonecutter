package dev.kikugie.stonecutter.controller.file

import org.gradle.api.NamedDomainObjectContainer

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class FileContainerDsl

@StonecutterExperimentalFilesAPI @FileContainerDsl
public abstract class FileHandlerContainer {
    protected abstract val handlers: NamedDomainObjectContainer<FileHandlerBuilder>

    internal operator fun get(extension: String): FileHandlerBuilder? = handlers.findByName(extension)

    public fun configure(vararg formats: String, action: FileHandlerBuilder.() -> Unit): Unit = synchronized(handlers) {
        for (ext in formats) if (ext in handlers.names) handlers.named(ext, action) else handlers.register(ext, action)
    }

    public fun configureIfPresent(vararg formats: String, action: FileHandlerBuilder.() -> Unit): Unit = synchronized(handlers) {
        for (ext in formats) if (ext in handlers.names) handlers.named(ext, action)
    }

    public fun configureIfAbsent(vararg formats: String, action: FileHandlerBuilder.() -> Unit): Unit = synchronized(handlers) {
        for (ext in formats) if (ext !in handlers.names) handlers.register(ext, action)
    }
}