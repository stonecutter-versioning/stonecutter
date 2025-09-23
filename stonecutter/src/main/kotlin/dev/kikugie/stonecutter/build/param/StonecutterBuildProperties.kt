package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.ext.ConstantContainer.Companion.constantContainer
import dev.kikugie.stonecutter.build.ext.DependencyContainer.Companion.dependencyContainer
import dev.kikugie.stonecutter.build.ext.ReplacementContainer.Companion.replacementContainer
import dev.kikugie.stonecutter.build.ext.SwapContainer.Companion.swapContainer
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.ext.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.util.flags
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import javax.inject.Inject
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class)
public abstract class StonecutterBuildProperties @Inject internal constructor(
    override val node: ProjectNode,
    flags: StonecutterFlags,
    objects: ObjectFactory,
    factory: ProviderFactory
) : Named, StonecutterBuildExtension, VersionOperations<ParsedVersion> by LenientOperations {
    internal val params: StonecutterBuildParameters = objects.newInstance<StonecutterBuildParameters>(flags, current.version)
    override val filters: PatternFilterable = PatternSet()

    init {
        with(extensions) {
            constantContainer("constants", params.constants)
            swapContainer("swaps", params.swaps)
            dependencyContainer("dependencies", params.dummyDependencies)
            replacementContainer("replacements", params::addString, params::addRegex)
            add(FlagContainer::class, "flags", tree.project.the<StonecutterControllerExtension>().flags)
        }
    }

    override fun getName(): String = "StonecutterBuild@${node.hierarchy}"
}

