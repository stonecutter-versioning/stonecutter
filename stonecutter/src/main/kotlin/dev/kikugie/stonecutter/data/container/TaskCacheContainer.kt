package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import javax.inject.Inject

@StonecutterExperimentalFilesAPI
internal abstract class TaskCacheContainer @Inject constructor(objects: ObjectFactory) : BuildService<TaskCacheContainer.Parameters>, OperationCompletionListener {
    interface Parameters : BuildServiceParameters {
        val handlers: Property<FileHandlerContainer>
    }
    val handlers get() = parameters.handlers.get()

    /**
     * This method is needed to make the build service persist with configuration cache.
     */
    override fun onFinish(event: FinishEvent?) {
        // no-op
    }
}