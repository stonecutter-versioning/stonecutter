package dev.kikugie.stonecutter.controller.file

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class FileHandlerDsl

@StonecutterExperimentalFilesAPI @FileHandlerDsl
public abstract class FileHandlerBuilder @Inject constructor(private val objects: ObjectFactory) {
    @get:Nested public abstract val scanner: Property<ScannerBuilder>

    public fun scanner(action: ScannerBuilder.() -> Unit) {
        val builder = scanner.orNull ?: objects.newInstance<ScannerBuilder>()
        scanner.set(builder.apply(action))
    }
}