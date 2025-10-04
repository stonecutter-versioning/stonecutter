package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

@StonecutterExperimentalFilesAPI
internal abstract class TaskCacheContainer : BuildService<BuildServiceParameters.None> {
    abstract val handlers: FileHandlerContainer
}