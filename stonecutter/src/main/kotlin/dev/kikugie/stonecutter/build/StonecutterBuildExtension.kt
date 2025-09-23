package dev.kikugie.stonecutter.build

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildConfig
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import groovy.lang.Closure
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.util.PatternFilterable

/**Stonecutter plugin available in `build.gradle[.kts]`.*/
@StonecutterAPI
public interface StonecutterBuildExtension : ExtensionAware {
    public val node: ProjectNode
    public val branch: ProjectBranch get() = node.branch
    public val tree: ProjectTree get() = branch.tree

    /**
     * Metadata of the active subproject, which has root `src/` sources assigned to it.
     */
    public val active: StonecutterProject?
        get() = tree.current

    /**
     * Metadata of the subproject assigned to this instance of the versioned buildscript.
     * Can be active, which can be checked with `stonecutter.current.isActive`
     * or `stonecutter.current == stonecutter.active`.
     */
    public val current: StonecutterProject
        get() = node.metadata

    /**
     * All subproject metadata entries in this branch (or project when a single branch is used).
     * Entries maintain instance identity:
     * ```kotlin
     * assert(stonecutter.versions.first { it.isActive } === stonecutter.active)
     * ```
     */
    public val versions: Collection<StonecutterProject>
        get() = branch.versions

    public val filters: PatternFilterable

    /**Provides [VersionOperations], which work strictly with [SemanticVersion]s.*/
    // TODO: Convert to extension
    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    /**
     * Read-only configuration flags container passed from `stonecutter.gradle[.kts]`.
     * Can be used to retrieve default and custom configuration values.
     */
    // TODO: Convert to extension
    public val flags: FlagContainer

    /**
     * Structured task container for each stage of file processing.
     * This can be used to programmatically configure task dependencies
     * when the automatic method doesn't work correctly.
     */
    // TODO: Convert to extension
    public val tasks: StonecutterBuildTasks

    public infix fun flags(action: FlagContainer.() -> Unit): Unit = flags.action()
    public fun flags(action: Closure<*>): Unit = flags(action::call)

    public infix fun tasks(action: StonecutterBuildTasks.() -> Unit): Unit = tasks.action()
    public fun tasks(action: Closure<*>): Unit = tasks(action::call)
}