@file:OptIn(StonecutterInternalAPI::class)

package dev.kikugie.stonecutter.build.data

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.util.newInstance
import dev.kikugie.stitcher.transform.replacement.RegexReplacement
import dev.kikugie.stitcher.transform.replacement.ReplacementBuilder
import dev.kikugie.stitcher.transform.replacement.StringReplacement
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

internal abstract class StonecutterBuildData @Inject constructor(
    controller: StonecutterControllerExtension,
    current: Identifier,
    providers: ProviderFactory
) {
    @get:Input public abstract val constants: MapProperty<Identifier, Boolean>
    @get:Input public abstract val swaps: MapProperty<Identifier, String>
    @get:Input public abstract val dependencies: MapProperty<Identifier, AnyVersion>

    @get:Nested public abstract val stringReplacements: ListProperty<StringReplacementSpec>
    @get:Nested public abstract val regexReplacements: ListProperty<RegexReplacementSpec>

    @get:Internal internal abstract val dummyDependencies: MapProperty<Identifier, AnyVersion>

    @get:Inject protected abstract val objects: ObjectFactory

    private val stringReplacementBuilder: ReplacementBuilder<StringReplacement> = ReplacementBuilder.string()
    private val regexReplacementBuilder: ReplacementBuilder<RegexReplacement> = ReplacementBuilder.regex()

    init {
        constants.set(mutableMapOf())
        swaps.set(mutableMapOf())
        dependencies.set(providers.provider { patchImplicitDependency(dummyDependencies, controller.flags[StonecutterFlag.IMPLICIT_RECEIVER], current) })

        dummyDependencies.set(mutableMapOf())

        stringReplacements.set(providers.provider { stringReplacementBuilder.build().map(objects::stringSpec) })
        regexReplacements.set(providers.provider { regexReplacementBuilder.build().map(objects::regexSpec) })
    }

    internal fun addString(repl: StringReplacement): Unit = stringReplacementBuilder.add(repl)
    internal fun addRegex(repl: RegexReplacement): Unit = regexReplacementBuilder.add(repl)

    public interface StringReplacementSpec {
        @get:Input public val target: Property<String>
        @get:Input public val sources: SetProperty<String>
        @get:Input @get:Optional public val identifier: Property<Identifier>

        public fun build(): StringReplacement =
            StringReplacement(target.get(), sources.get(), identifier.orNull)
    }

    public interface RegexReplacementSpec {
        @get:Input public val target: Property<String>
        @get:Input public val pattern: Property<String>
        @get:Input @get:Optional public val flags: SetProperty<RegexOption>
        @get:Input @get:Optional public val identifier: Property<Identifier>

        public fun build(): RegexReplacement =
            RegexReplacement(target.get(), pattern.get(), emptySet(), identifier.orNull)
    }
}

private fun ObjectFactory.stringSpec(repl: StringReplacement): StonecutterBuildData.StringReplacementSpec =
    newInstance { target.set(repl.target); sources.set(repl.sources); identifier.set(repl.identifier) }

private fun ObjectFactory.regexSpec(repl: RegexReplacement): StonecutterBuildData.RegexReplacementSpec =
    newInstance { target.set(repl.target); pattern.set(repl.pattern); flags.set(repl.flags); identifier.set(repl.identifier) }

private fun patchImplicitDependency(
    prop: MapProperty<Identifier, AnyVersion>,
    key: Identifier,
    version: AnyVersion
): Map<Identifier, AnyVersion> = buildMap {
    putAll(prop.get())
    val implicit = getOrDefault(key, version)
    this[""] = implicit
    this[key] = implicit
}