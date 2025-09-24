package dev.kikugie.stonecutter.data.tree.builder

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.data.StonecutterProject
import groovy.lang.Closure
import org.gradle.api.provider.Property

/**
 * Provides functionality to construct and manage a hierarchical structure of project branches.
 * Serves as the root of the project tree and facilitates branching operations and shared configurations.
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
 */
@StonecutterAPI
public abstract class TreeBuilder : BranchBuilder() {
    /**
     * Represents a reset point to maintain code consistency on commits.
     * Must be one of the registered **project names**.
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
     * @see mapBuilds
     * @see NodeBuilder.buildscript
     */
    public abstract val centralScript: Property<String>

    /**
     * Configures whenever `stonecutter.gradle.kts` or `stonecutter.gradle` is used.
     * When unset, prefers the `.kts` variant, but uses `.gradle` one if it already exists.
     */
    public abstract val kotlinController: Property<Boolean>

    /**
     * Creates a new project branch with the given [name], copying all already registered versions.
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
     */
    public fun branch(name: Identifier): Unit =
        branch(name) { inherit() }

    /**
     * Defines a new branch with the provided [name] and configurations specified in the [closure].
     * The [closure] allows customizing branch settings using the [BranchBuilder].
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
     */
    public fun branch(name: Identifier, closure: Closure<BranchBuilder>): Unit =
        branch(name, closure::call)

    /**
     * Defines a new branch identified by [name] and configures it using the [action] block.
     * The [action] block provides access to the branch-specific operations via [BranchBuilder].
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
     */
    public abstract fun branch(name: Identifier, action: BranchBuilder.() -> Unit)

    /**
     * Defines the build script naming for each version.
     * The [action] accepts a branch [name][Identifier] and the version [metadata][StonecutterProject]
     * as parameters, returning the build script filename.
     *
     * @see <a href="https://stonecutter.kikugie.dev/wiki/config/settings#naming-strategy">Wiki</a>
     */
    public abstract fun mapBuilds(action: (Identifier, StonecutterProject) -> String)
}

/**
 * Represents a given branch in the project tree.
 *
 * Stonecutter allows multiple versioned subprojects to have syncronized active versions
 * and safe cross-project dependencies, which is achieved with the branch structure.
 *
 * A branch builder can be created with [TreeBuilder.branch], which handles the subproject registration.
 * The tree builder is on its own a branch, referred to as the **root**.
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/branches">Wiki</a>
 */
@StonecutterAPI
public abstract class BranchBuilder {
    /**
     * Configures the build script name for this branch.
     * It cannot be set to `stonecutter.gradle[.kts]` and can't be modified on the root branch.
     * When unset, prefers `branch.gradle.kts`, but uses `branch.gradle` if it already exists.
     */
    public abstract val branchScript: Property<String>

    /**
     * Adds a [StonecutterProject] to the builder for the specified [name] and [version].
     *
     * @param name the **unique** identifier for the project.
     * @param version the version associated with the project.
     * @return a [NodeBuilder] for this configuration.
     */
    @Deprecated("Use the fully spelled version", replaceWith = ReplaceWith("version(name, version)"))
    public fun vers(name: Identifier, version: Version): NodeBuilder =
        version(name, version)

    /**
     * Creates a [NodeBuilder] with a list containing a single [StonecutterProject]
     * where both the project identifier and version are derived from [version].
     *
     * @param version the version used to construct both the project identifier and version.
     * @return a [NodeBuilder] for this configuration.
     */
    public fun version(version: Version): NodeBuilder =
        versions(listOf(StonecutterProject(version, version)))

    /**
     * Adds a [StonecutterProject] to the builder for the specified [name] and [version].
     *
     * @param name the **unique** identifier for the project.
     * @param version the version associated with the project.
     * @return a [NodeBuilder] for this configuration.
     */
    public fun version(name: Identifier, version: Version): NodeBuilder =
        versions(listOf(StonecutterProject(name, version)))

    /**
     * Adds multiple [StonecutterProject] entries to the builder based on the provided [versions] map.
     * Each entry combines an identifier and version into a corresponding project configuration.
     *
     * @param versions a map of identifiers to their associated versions.
     * @return a [NodeBuilder] for this configuration.
     */
    public infix fun versions(versions: Map<Identifier, Version>): NodeBuilder =
        versions(versions.map { (k, v) -> StonecutterProject(k, v) })

    /**
     * Adds multiple [StonecutterProject] entries to the builder based on the provided [versions] map.
     * Each entry combines an identifier and version into a corresponding project configuration.
     *
     * @param versions a map of identifiers to their associated versions.
     * @return a [NodeBuilder] for this configuration.
     */
    public infix fun versions(versions: Iterable<Identifier>): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    /**
     * Adds multiple [StonecutterProject] entries to the builder based on the provided [versions] map.
     * Each entry combines an identifier and version into a corresponding project configuration.
     *
     * @param versions a map of identifiers to their associated versions.
     * @return a [NodeBuilder] for this configuration.
     */
    public fun versions(vararg versions: Identifier): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    /**
     * Adds multiple [StonecutterProject] entries to the builder based on the provided [versions] map.
     * Each entry combines an identifier and version into a corresponding project configuration.
     *
     * @param versions a map of identifiers to their associated versions.
     * @return a [NodeBuilder] for this configuration.
     */
    @JvmName("versionPairs")
    public infix fun versions(versions: Iterable<Pair<Identifier, Version>>): NodeBuilder =
        versions(versions.map { (p, v) -> StonecutterProject(p, v) })

    /**
     * Adds multiple [StonecutterProject] entries to the builder based on the provided [versions] map.
     * Each entry combines an identifier and version into a corresponding project configuration.
     *
     * @param versions a map of identifiers to their associated versions.
     * @return a [NodeBuilder] for this configuration.
     */
    @JvmName("versionPairs")
    public fun versions(vararg versions: Pair<Identifier, Version>): NodeBuilder =
        versions(versions.map { (p, v) -> StonecutterProject(p, v) })

    /**
     * Copies versions registered in the root branch into this one.
     *
     * @throws UnsupportedOperationException if called on the root branch.
     */
    public abstract fun inherit()
    protected abstract fun versions(versions: List<StonecutterProject>): NodeBuilder
}

/**
 * Represents a **collection** of registered nodes in a given branch,
 * providing a way to override the build script filename.
 *
 * Node sets registered later override the buildscripts for the previous ones:
 * ```kt
 *  stonecutter {
 *      create(rootProject) {
 *          versions("1", "2", "3")
 *              .buildscript("A.gradle.kts")
 *          versions("3", "4")
 *              .buildscript("B.gradle.kts")
 *          // '3' will have `B.gradle.kts`
 *      }
 *  }
 * ```
 */
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
    public fun buildscript(name: String): NodeBuilder =
        apply { buildscript.set(name) }
}