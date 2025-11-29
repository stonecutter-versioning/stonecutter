package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stonecutter.controller.file.FileHandlerService.Parameters
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import javax.inject.Inject

internal abstract class FileHandlerService @Inject constructor(objects: ObjectFactory) : BuildService<Parameters> {
    interface Parameters : BuildServiceParameters {
        val handlers: MapProperty<String, HandlerModel>
    }

    companion object {
        const val NAME: String = "StonecutterFileHandlers"
    }
}