package dev.kikugie.stonecutter.build.data

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.config.ConstantContainer
import dev.kikugie.stonecutter.build.config.DependencyContainer
import dev.kikugie.stonecutter.build.config.ReplacementContainerImpl
import dev.kikugie.stonecutter.build.config.SwapContainer
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagsView
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.version.LenientOperations
import dev.kikugie.stonecutter.data.version.VersionOperations
import dev.kikugie.semver.data.Version
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import javax.inject.Inject

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildConfiguration @Inject constructor(
    override val node: ProjectNode,
    factory: ProviderFactory,
    objects: ObjectFactory
) : StonecutterBuildExtension, Named, VersionOperations<Version> by LenientOperations {
    internal val controller: StonecutterControllerExtension = node.tree.project.the()
    internal val data: StonecutterBuildData = objects.newInstance(controller, current.project, factory)

    override val swaps: SwapContainer = objects.newInstance(data.swaps)
    override val constants: ConstantContainer = objects.newInstance(data.constants)
    override val dependencies: DependencyContainer = objects.newInstance(data.dependencies)
    override val replacements: ReplacementContainerImpl = objects.newInstance(data::addString, data::addRegex)
    override val filters: PatternFilterable = PatternSet()

    override val flags: StonecutterFlagsView = controller.flags
    override val tasks: StonecutterBuildTasks
        get() = throw UnsupportedOperationException("Build tasks are not available in the controller")
}