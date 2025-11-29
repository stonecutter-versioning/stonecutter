package dev.kikugie.stonecutter.settings

import dev.kikugie.stonecutter.ProjectReference
import dev.kikugie.stonecutter.data.version.SemanticOperations
import dev.kikugie.stonecutter.data.version.VersionOperations
import dev.kikugie.stonecutter.settings.tree.SerializedTree
import dev.kikugie.stonecutter.settings.tree.TreeBuilder
import dev.kikugie.stonecutter.settings.tree.TreeBuilderImpl
import dev.kikugie.stonecutter.util.SCJSON
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import java.io.File

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class SettingsDsl

@SettingsDsl
public abstract class StonecutterSettingsExtension(protected val objects: ObjectFactory) : VersionOperations<Version> {
    /**
     * Defines a shared [Action] for configuring instances of [TreeBuilder].
     *
     * This action is applied when using [create] methods with no explicit configuration,
     * and can be added manually with `shared.execute(this)` in a [TreeBuilder] scope.
     */
    public var shared: Action<TreeBuilder> = Action {}
        private set

    /**
     * Defines the name for the shared buildscript for all versions.
     * It cannot be set to `stonecutter.gradle[.kts]`.
     * When unset, prefers `build.gradle.kts`, but uses `build.gradle` if it already exists.
     *
     * This value applies to all registered trees, and can be overridden with
     * [TreeBuilder.centralScript], [TreeBuilder.mapBuilds] or [NodeBuilder.buildscript][dev.kikugie.stonecutter.data.tree.builder.NodeBuilder.buildscript].
     */
    public abstract val centralScript: Property<String>

    /**
     * Configures whenever `stonecutter.gradle.kts` or `stonecutter.gradle` is used.
     * When unset, prefers the `.kts` variant, but uses `.gradle` one if it already exists.
     *
     * This value applies to all registered trees, and can be overridden with
     * [TreeBuilder.kotlinController]
     */
    public abstract val kotlinController: Property<Boolean>

    public val semantics: VersionOperations<SemanticVersion> get() = SemanticOperations

    /* Shared configuration */
    /**
     * Defines a shared [Action] for configuring instances of [TreeBuilder].
     *
     * This action is applied when using [create] methods with no explicit configuration,
     * and can be added manually with `shared.execute(this)` in a [TreeBuilder] scope.
     */
    public fun shared(action: Action<TreeBuilder>) {
        shared = action
    }

    /* File configuration */
    /**
     * Configures the specified [project] to be versioned with setup provided by [file].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(project: ProjectReference, file: File, action: Action<TreeBuilder> = shared): Unit =
        create(listOf(project), readTreeSettings(file, action))

    /**
     * Configures the specified [projects] to be versioned with setup provided by [file].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(vararg projects: ProjectReference, file: File, action: Action<TreeBuilder> = shared): Unit =
        create(projects.asIterable(), readTreeSettings(file, action))

    /**
     * Configures the specified [projects] to be versioned with setup provided by [file].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(projects: Iterable<ProjectReference>, file: File, action: Action<TreeBuilder> = shared): Unit =
        create(projects, readTreeSettings(file, action))

    /* Action configuration */
    /**
     * Configures the specified [project] to be versioned with setup provided by [action] or [shared].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(project: ProjectReference, action: Action<TreeBuilder> = shared): Unit =
        create(listOf(project), action)

    /**
     * Configures the specified [projects] to be versioned with setup provided by [action] or [shared].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(vararg projects: ProjectReference, action: Action<TreeBuilder> = shared): Unit =
        create(projects.asIterable(), action)

    /**
     * Configures the specified [projects] to be versioned with setup provided by [action] or [shared].
     * @see ProjectReference
     */
    @JvmOverloads
    public fun create(projects: Iterable<ProjectReference>, action: Action<TreeBuilder> = shared): Unit =
        projects.forEach { create(it, objects.newInstance<TreeBuilderImpl>().also(action::execute)) }

    /* Base configuration */
    protected abstract fun create(ref: ProjectReference, builder: TreeBuilder)
}

@OptIn(ExperimentalSerializationApi::class)
private fun readTreeSettings(file: File, action: Action<TreeBuilder>): Action<TreeBuilder> {
    require(file.extension.let { it == "json" || it == "json5" }) {
        "Version setup file must be in JSON or JSON5 format.\nSee https://stonecutter.kikugie.dev/wiki/config/projects."
    }
    return Action {
        file.inputStream().use { SCJSON.decodeFromStream<SerializedTree>(it) }.applyTo(this)
        action.execute(this)
    }
}