package dev.kikugie.stonecutter.controller.file

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import javax.inject.Inject

internal abstract class FileHandlerService @Inject constructor(objects: ObjectFactory) :
    BuildService<FileHandlerService.Parameters>, OperationCompletionListener {
    override fun onFinish(event: FinishEvent?) = Unit

    interface Parameters : BuildServiceParameters {
        val fileHandlers: MapProperty<String, FileHandlerBuilder>
    }

    companion object {
        const val NAME = "stonecutter-file-handlers"
    }
}