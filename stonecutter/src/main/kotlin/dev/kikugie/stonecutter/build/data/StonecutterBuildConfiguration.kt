package dev.kikugie.stonecutter.build.data

import dev.kikugie.semver.data.Version
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.config.ConstantContainer
import dev.kikugie.stonecutter.build.config.DependencyContainer
import dev.kikugie.stonecutter.build.config.ReplacementContainerImpl
import dev.kikugie.stonecutter.build.config.SwapContainer
import dev.kikugie.stonecutter.build.task.GradleProblemReporter
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.build.task.forFile
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.file.HandlerModel
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagsView
import dev.kikugie.stonecutter.controller.tree.ParametersModel
import dev.kikugie.stonecutter.data.container.GradleContainerExtension.Companion.getContainer
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.version.LenientOperations
import dev.kikugie.stonecutter.data.version.VersionOperations
import dev.kikugie.stonecutter.util.overwriteText
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import java.io.File
import javax.inject.Inject

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildConfiguration @Inject constructor(
    override val node: ProjectNode,
    factory: ProviderFactory,
    objects: ObjectFactory
) : Named, StonecutterBuildExtension, VersionOperations<Version> by LenientOperations {
    internal val controller: StonecutterControllerExtension = node.tree.project.the()
    internal val data: StonecutterBuildData = objects.newInstance(controller, current.version, factory)

    override val swaps: SwapContainer = objects.newInstance(data.swaps)
    override val constants: ConstantContainer = objects.newInstance(data.constants)
    override val dependencies: DependencyContainer = objects.newInstance(data.dependencies)
    override val replacements: ReplacementContainerImpl = objects.newInstance(data::addString, data::addRegex)
    override val filters: PatternFilterable = PatternSet()

    override val flags: StonecutterFlagsView = controller.flags
    override val tasks: StonecutterBuildTasks
        get() = throw UnsupportedOperationException("Build tasks are not available in the controller")

    @OptIn(StonecutterExperimentalAPI::class)
    override fun process(file: File, destination: String): File {
        // Jankiest code ever
        val project = node.project
        val handlers = project.gradle.getContainer<FileHandlerContainer>()
            .handlers[file.extension.lowercase()].let { mapOf(it.name to HandlerModel(it)) }
        val parameters = ParametersModel(data).forFile(file.toPath(), handlers)!!
        val reporter = GradleProblemReporter(node.project.logger) { true }

        val contents = reporter.run { dev.kikugie.stitcher.process(file.toPath(), file.readText(), parameters, reporter) }
        val output = project.layout.projectDirectory.file(destination)
            .also(project.providers::fileContents).asFile
        output.overwriteText(contents, true)
        return output
    }
}