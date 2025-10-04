package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import org.gradle.api.model.ObjectFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@StonecutterExperimentalFilesAPI
internal abstract class TaskCacheContainer @Inject constructor(objects: ObjectFactory) : BuildService<BuildServiceParameters.None> {
    val handlers: FileHandlerContainer = objects.newInstance<FileHandlerContainer>()
}