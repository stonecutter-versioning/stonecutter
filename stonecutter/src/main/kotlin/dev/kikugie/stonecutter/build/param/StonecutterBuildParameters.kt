package dev.kikugie.stonecutter.build.param

import dev.kikugie.stitcher.transform.replacement.RegexReplacement
import dev.kikugie.stitcher.transform.replacement.ReplacementBuilder
import dev.kikugie.stitcher.transform.replacement.StringReplacement
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
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
import javax.inject.Inject

@OptIn(StonecutterInternalAPI::class)
private fun ObjectFactory.stringSpec(repl: StringReplacement): StonecutterBuildParameters.StringReplacementSpec =
    newInstance { target.set(repl.target); sources.set(repl.sources); identifier.set(repl.identifier) }

@OptIn(StonecutterInternalAPI::class)
private fun ObjectFactory.regexSpec(repl: RegexReplacement): StonecutterBuildParameters.RegexReplacementSpec =
    newInstance { target.set(repl.target); pattern.set(repl.pattern.pattern); flags.set(repl.pattern.options); identifier.set(repl.identifier) }

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
public abstract class StonecutterBuildParameters @Inject internal constructor(
    flags: StonecutterFlags,
    current: Version,
    objects: ObjectFactory,
    factory: ProviderFactory
) {
    @get:Input public abstract val constants: MapProperty<Identifier, Boolean>
    @get:Input public abstract val swaps: MapProperty<Identifier, String>
    @get:Input public abstract val dependencies: MapProperty<Identifier, Version>

    @get:Nested public abstract val stringReplacements: ListProperty<StringReplacementSpec>
    @get:Nested public abstract val regexReplacements: ListProperty<RegexReplacementSpec>

    @get:Internal internal abstract val dummyDependencies: MapProperty<Identifier, Version>

    private val stringReplacementBuilder: ReplacementBuilder<StringReplacement>
    private val regexReplacementBuilder: ReplacementBuilder<RegexReplacement>

    init {
        val replacementIdentifierPool = mutableSetOf<Identifier>()
        stringReplacementBuilder = ReplacementBuilder.string(replacementIdentifierPool)
        regexReplacementBuilder = ReplacementBuilder.regex(replacementIdentifierPool)

        constants.set(mutableMapOf())
        swaps.set(mutableMapOf())
        dependencies.set(factory.provider { patchImplicitDependency(dummyDependencies, flags[StonecutterFlag.IMPLICIT_RECEIVER], current) })

        dummyDependencies.set(mutableMapOf())

        stringReplacements.set(factory.provider { stringReplacementBuilder.build().map(objects::stringSpec) })
        regexReplacements.set(factory.provider { regexReplacementBuilder.build().map(objects::regexSpec) })
    }

    internal fun addString(repl: StringReplacement): Unit = stringReplacementBuilder.add(repl).getOrThrow()
    internal fun addRegex(repl: RegexReplacement): Unit = regexReplacementBuilder.add(repl).getOrThrow()

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