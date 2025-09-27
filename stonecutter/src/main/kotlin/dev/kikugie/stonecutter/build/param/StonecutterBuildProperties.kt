package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.ext.ConstantContainer
import dev.kikugie.stonecutter.build.ext.DependencyContainer
import dev.kikugie.stonecutter.build.ext.ReplacementContainer
import dev.kikugie.stonecutter.build.ext.SwapContainer
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.ext.FlagContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import javax.inject.Inject
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildProperties @Inject internal constructor(override val node: ProjectNode, factory: ProviderFactory, objects: ObjectFactory)
    : Named, StonecutterBuildExtension, VersionOperations<ParsedVersion> by LenientOperations {
    private val controller = node.tree.project.the<StonecutterControllerExtension>()
    internal val params: StonecutterBuildParameters = objects.newInstance<StonecutterBuildParameters>(controller, current.version, factory)

    override val constants: ConstantContainer = ConstantContainer(factory, params.constants)
    override val swaps: SwapContainer = SwapContainer(factory, params.swaps)
    override val dependencies: DependencyContainer = DependencyContainer(factory, params.dummyDependencies)
    override val replacements: ReplacementContainer = ReplacementContainer(objects, params::addString, params::addRegex)
    override val filters: PatternFilterable = PatternSet()
    override val flags: FlagContainer = controller.flags
    override val tasks: StonecutterBuildTasks
        get() = throw UnsupportedOperationException("Build tasks are not available in the controller")

    override fun getName(): String = "StonecutterBuild@${node.hierarchy}"
}

