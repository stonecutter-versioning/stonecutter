@file:OptIn(StonecutterInternalAPI::class)

package dev.kikugie.stonecutter.build.param

import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.impl.StarCommentStrategy
import dev.kikugie.stitcher.transform.replacement.RegexReplacement
import dev.kikugie.stitcher.transform.replacement.ReplacementBuilder
import dev.kikugie.stitcher.transform.replacement.StringReplacement
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.file.FileHandlerBuilder
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.util.get
import dev.kikugie.stonecutter.util.invoke
import dev.kikugie.stonecutter.util.newInstance
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.get
import java.io.Serializable
import javax.inject.Inject

private fun ObjectFactory.stringSpec(repl: StringReplacement): StonecutterBuildParameters.StringReplacementSpec =
    newInstance { target.set(repl.target); sources.set(repl.sources); identifier.set(repl.identifier) }

private fun ObjectFactory.regexSpec(repl: RegexReplacement): StonecutterBuildParameters.RegexReplacementSpec =
    newInstance { target.set(repl.target); pattern.set(repl.pattern.pattern); flags.set(repl.pattern.options); identifier.set(repl.identifier) }

private fun StonecutterBuildParameters.RegexReplacementSpec.build(): RegexReplacement =
    RegexReplacement(target.get(), Regex(pattern.get()), identifier.orNull)

private fun StonecutterBuildParameters.StringReplacementSpec.build(): StringReplacement =
    StringReplacement(target.get(), sources.get(), identifier.orNull)

private fun patchImplicitDependency(prop: MapProperty<Identifier, Version>, key: Identifier, version: Version): Map<Identifier, Version> = buildMap {
    putAll(prop.get())
    val implicit = getOrDefault(key, version)
    this[""] = implicit
    this[key] = implicit
}

/**
 * Represents parameters used in the file processor in Gradle-cacheable form.
 *
 * This class is public to be accessible in [SCPrepareTask][dev.kikugie.stonecutter.process.SCPrepareTask].
 * However, the properties should not be modified directly, as it is likely to cause errors at task runtime.
 */
@StonecutterInternalAPI
public abstract class StonecutterBuildParameters @Inject internal constructor(private val ext: StonecutterControllerExtension, current: Version, factory: ProviderFactory) {
    @get:Input public abstract val constants: MapProperty<Identifier, Boolean>
    @get:Input public abstract val swaps: MapProperty<Identifier, String>
    @get:Input public abstract val dependencies: MapProperty<Identifier, Version>

    @get:Nested public abstract val stringReplacements: ListProperty<StringReplacementSpec>
    @get:Nested public abstract val regexReplacements: ListProperty<RegexReplacementSpec>

    @get:Internal internal abstract val dummyDependencies: MapProperty<Identifier, Version>

    @get:Inject protected abstract val objects: ObjectFactory

    private val stringReplacementBuilder: ReplacementBuilder<StringReplacement>
    private val regexReplacementBuilder: ReplacementBuilder<RegexReplacement>

    init {
        val replacementIdentifierPool = mutableSetOf<Identifier>()
        stringReplacementBuilder = ReplacementBuilder.string(replacementIdentifierPool)
        regexReplacementBuilder = ReplacementBuilder.regex(replacementIdentifierPool)

        constants.set(mutableMapOf())
        swaps.set(mutableMapOf())
        dependencies.set(factory.provider { patchImplicitDependency(dummyDependencies, ext.flags[StonecutterFlag.IMPLICIT_RECEIVER], current) })

        dummyDependencies.set(mutableMapOf())

        stringReplacements.set(factory.provider { stringReplacementBuilder.build().map(objects::stringSpec) })
        regexReplacements.set(factory.provider { regexReplacementBuilder.build().map(objects::regexSpec) })
    }

    internal fun addString(repl: StringReplacement): Unit = stringReplacementBuilder.add(repl).getOrThrow()
    internal fun addRegex(repl: RegexReplacement): Unit = regexReplacementBuilder.add(repl).getOrThrow()

    @OptIn(StonecutterExperimentalFilesAPI::class)
    internal fun toTransformParameters(): TransformParametersBuilder {
        val data = toBuildData()
        val scanners = mutableMapOf<String, ScannerAdapter.Factory>()
        val commenters = mutableMapOf<String, CommentingStrategy>()
        val uncommenters = mutableMapOf<String, UncommentingStrategy>()
        val swappers = mutableMapOf<String, SwappingStrategy>()

        for (name in ext.handlers.names) {
            val handler = ext.handlers[name]
            scanners[name] = handler.scanner().let { ScannerAdapter.Factory {
                input, sink -> ScannerAdapter(it.constructor().create(input), it.openers().toIntArray(), it.closers().toIntArray(), sink)
            } }
            commenters[name] = handler.commenter()
            uncommenters[name] = handler.uncommenter()
            swappers[name] = handler.swapper()
        }
        return TransformParametersBuilder(data.swaps, data.constants, data.dependencies, data.replacements, scanners, commenters, uncommenters, swappers)
    }

    internal fun toBuildData(): StonecutterBuildData {
        val constants = constants.get()
        val swaps = swaps.get()
        val dependencies = dependencies.get().mapValues { (_, it) -> LenientOperations.parse(it) }
        val replacements = stringReplacements.get().map { it.build() } + regexReplacements.get().map { it.build() }
        return StonecutterBuildData(constants, swaps, dependencies, replacements)
    }

    public interface StringReplacementSpec {
        @get:Input public val target: Property<String>
        @get:Input public val sources: SetProperty<String>
        @get:Input @get:Optional public val identifier: Property<Identifier>
    }

    public interface RegexReplacementSpec {
        @get:Input public val target: Property<String>
        @get:Input public val pattern: Property<String>
        @get:Input @get:Optional public val flags: SetProperty<RegexOption>
        @get:Input @get:Optional public val identifier: Property<Identifier>
    }
}