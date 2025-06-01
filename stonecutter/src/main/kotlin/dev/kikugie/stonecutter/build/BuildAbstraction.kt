package dev.kikugie.stonecutter.build

import dev.kikugie.stitcher.data.replacement.Replacement
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.container.ConfigurationService.Companion.of
import dev.kikugie.stonecutter.data.parameters.BuildParameters
import dev.kikugie.stonecutter.controller.ControllerAbstraction
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Contains logic for the versioned buildscript, which is separated to allow
 * global configuration by [ControllerAbstraction.parameters].
 * @property hierarchy Path of the corresponding project
 */
public abstract class BuildAbstraction(protected val hierarchy: ProjectHierarchy) :
    SwapVariants, ConstantVariants, DependencyVariants, FilterVariants, ReplacementVariants {
    protected val data: BuildParameters = checkNotNull(StonecutterPlugin.SERVICE.of(hierarchy).build) {
        "Stonecutter build parameters not found for $hierarchy. Present keys:\n%s"
            .format(StonecutterPlugin.SERVICE().parameters.buildParameters.keysToString())
    }

    override val swaps: MutableMap<Identifier, String> = CheckedMap(data.swaps) { k, _ -> k.validateId() }
    override val consts: MutableMap<Identifier, Boolean> = CheckedMap(data.constants) { k, _ -> k.validateId() }
    override val dependencies: DependencyVariants.VersionMap = CheckedMap(data.dependencies) { k, _ -> k.validateId() }
        .let(DependencyVariants::VersionMap)

    override val replacements: Collection<Replacement>
        get() = data.replacements

    override fun replacement(
        direction: Boolean,
        source: String,
        target: String,
        phase: String,
        identifier: Identifier?
    ) {
        if (identifier != null) require(identifier.isValid()) { "Invalid identifier: '$identifier'" }
        val realPhase = when (phase.lowercase()) {
            "first" -> ReplacementPhase.FIRST
            "last" -> ReplacementPhase.LAST
            else -> throw IllegalArgumentException("Invalid phase: '$phase', must be either 'FIRST' or 'LAST'")
        }
        if (direction) data.replacements.addString(source, target, realPhase, identifier)
        else data.replacements.addString(target, source, realPhase, identifier)
    }

    override fun replacement(
        direction: Boolean,
        sourcePattern: String,
        targetValue: String,
        targetPattern: String,
        sourceValue: String,
        phase: String,
        identifier: Identifier?
    ) {
        if (identifier != null) require(identifier.isValid()) { "Invalid identifier: '$identifier'" }
        val realPhase = when (phase.lowercase()) {
            "first" -> ReplacementPhase.FIRST
            "last" -> ReplacementPhase.LAST
            else -> throw IllegalArgumentException("Invalid phase: '$phase', must be either 'FIRST' or 'LAST'")
        }
        if (direction) data.replacements.addRegex(sourcePattern.toRegex(), targetValue, realPhase, identifier)
        else data.replacements.addRegex(targetPattern.toRegex(), sourceValue, realPhase, identifier)
    }

    override fun allowExtensions(extensions: Iterable<String>) {
        data.extensions += extensions
    }

    override fun overrideExtensions(extensions: Iterable<String>): Unit =
        data.extensions.clear() then allowExtensions(extensions)

    override fun excludeFiles(files: Iterable<String>): Unit = files.forEach {
        require(it.startsWith("src/")) { "File path must start with 'src/': $it" }
        Path(it).normalize().invariantSeparatorsPathString.let(data.exclusions::add)
    }

    internal fun from(other: BuildAbstraction): Unit = with(data) {
        swaps.putAll(other.data.swaps)
        constants.putAll(other.data.constants)
        dependencies.putAll(other.data.dependencies)
        extensions.addAll(other.data.extensions)
        exclusions.addAll(other.data.exclusions)
    }
}