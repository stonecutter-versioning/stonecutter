package dev.kikugie.stonecutter.settings.tree

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.StonecutterProject
import org.gradle.api.Action
import org.gradle.api.provider.Property

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class TreeBuilderDsl

/**
 * Provides functionality to construct and manage a hierarchical structure of project branches.
 * Serves as the root of the project tree and facilitates branching operations and shared configurations.
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
 */
@TreeBuilderDsl
public abstract class TreeBuilder : BranchBuilder() {
    /**
     * Represents a reset point to maintain code consistency on commits.
     * Must be one of the registered project names.
     * When unset, the first registered version is used.
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/glossary#vcs-version">Glossary</a>
     */
    public abstract val vcsVersion: Property<String>

    /**
    * Defines the name for the shared buildscript for all versions.
    * It cannot be set to `stonecutter.gradle[.kts]`.
    * When unset, prefers `build.gradle.kts`, but uses `build.gradle` if it already exists.
    *
    * @see [mapBuilds]
    * @see [NodeBuilder.buildscript]
    */
    public abstract val centralScript: Property<String>

    /**
     * Configures whenever `stonecutter.gradle.kts` or `stonecutter.gradle` is used.
     * When unset, prefers the `.kts` variant, but uses `.gradle` one if it already exists.
     */
    public abstract val kotlinController: Property<Boolean>

    @Deprecated("Root branch has no parent to inherit from", level = DeprecationLevel.HIDDEN)
    override fun inherit(): Unit = throw UnsupportedOperationException()

    /**
     * Creates a new project branch with the given [name], copying all already registered versions.
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
     */
    public fun branch(name: Identifier): Unit = branch(name) { inherit() }

    /**
     * Defines a new branch identified by [name] and configures it using the [config] block.
     * The [config] block provides access to the branch-specific operations via [BranchBuilder].
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
     */
    public abstract fun branch(name: Identifier, config: Action<BranchBuilder>)

    /**
     * Defines the build script naming for each version.
     * The [mapping] accepts a branch [name][Identifier] and the version [metadata][StonecutterProject]
     * as parameters, returning the build script filename.
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/settings#naming-strategy">Wiki</a>
     */
    public abstract fun mapBuilds(mapping: (branch: Identifier, node: StonecutterProject) -> String)
}

/**
 * Builder class for configuring Stonecutter project branches.
 *
 * This class provides functionality to define branch-specific scripts via [branchScript],
 * configure versions using multiple overloads of the [versions] method, and perform
 * inheritance using [inherit].
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
 */
@TreeBuilderDsl
public abstract class BranchBuilder {

    /**Specifies a buildscript filename for the branch. Has no effect on the main branch.*/
    @Deprecated("Not fully supported :P")
    public abstract val branchScript: Property<String>

    /**Registers a new subproject with the same name and version.*/
    public fun version(version: AnyVersion): NodeBuilder =
        versions(listOf(StonecutterProject(version, version)))

    /**Registers a new subproject with distinct [name][project] and [version].*/
    public fun version(project: Identifier, version: AnyVersion): NodeBuilder =
        versions(listOf(StonecutterProject(project, version)))

    /**Registers multiple subprojects based on the name-version [entries].*/
    public infix fun versions(entries: Map<Identifier, AnyVersion>): NodeBuilder =
        versions(entries.map { (k, v) -> StonecutterProject(k, v) })

    /**Registers multiple subprojects based with names matching the [versions].*/
    public infix fun versions(versions: Iterable<Identifier>): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    /**Registers multiple subprojects based with names matching the [versions].*/
    public fun versions(vararg versions: Identifier): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    /**Registers multiple subprojects based on the name-version [entries].*/
    @JvmName("versionPairs")
    public infix fun versions(entries: Iterable<Pair<Identifier, AnyVersion>>): NodeBuilder =
        versions(entries.map { (p, v) -> StonecutterProject(p, v) })

    /**Registers multiple subprojects based on the name-version [entries].*/
    @JvmName("versionPairs")
    public fun versions(vararg entries: Pair<Identifier, AnyVersion>): NodeBuilder =
        versions(entries.map { (p, v) -> StonecutterProject(p, v) })

    /**Copies the registered versions from the main branch.*/
    public abstract fun inherit()

    /**Registers parsed subprojects.*/
    public abstract fun versions(entries: List<StonecutterProject>): NodeBuilder
}

/**
 * Represents a collection of registered nodes in a given branch,
 * providing a way to override the build script filename.
 *
 * Node sets registered later override the buildscripts for the previous ones.
 */

@TreeBuilderDsl
public abstract class NodeBuilder {
    /**
     * Configures the build script filename used for this project set.
     * It cannot be set to `stonecutter.gradle[.kts]`.
     * When unset, uses [TreeBuilder.mapBuilds] or [TreeBuilder.centralScript] if the former is also unset..
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/settings#name-overrides">Wiki</a>
     */
    public abstract val buildscript: Property<String>

    /**
     * Sets the build script filename for the project set to the provided [name].
     */
    public fun buildscript(name: String): NodeBuilder = apply {
        buildscript.set(name)
    }
}