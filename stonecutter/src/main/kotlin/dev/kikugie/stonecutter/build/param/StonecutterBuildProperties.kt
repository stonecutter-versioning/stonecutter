package dev.kikugie.stonecutter.build.param

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.ext.ConstantContainer.Companion.constantContainer
import dev.kikugie.stonecutter.build.ext.DependencyContainer.Companion.dependencyContainer
import dev.kikugie.stonecutter.build.ext.ReplacementContainer.Companion.replacementContainer
import dev.kikugie.stonecutter.build.ext.SwapContainer.Companion.swapContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class)
public abstract class StonecutterBuildProperties @Inject constructor(
    override val node: ProjectNode,
    objects: ObjectFactory,
    factory: ProviderFactory
) : StonecutterBuildExtension, Named, VersionOperations<ParsedVersion> by LenientOperations {
    internal val params: StonecutterBuildParameters = objects.newInstance<StonecutterBuildParameters>(flags, current.version)

    init {
        with(extensions) {
            constantContainer("constants", params.constants)
            swapContainer("swaps", params.swaps)
            dependencyContainer("dependencies", params.dummyDependencies)
            replacementContainer("replacements", params::addString, params::addRegex)
        }
    }

    override fun getName(): String = "StonecutterBuild@${node.hierarchy}"
}

