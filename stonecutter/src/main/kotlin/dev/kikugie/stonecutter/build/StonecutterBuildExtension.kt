package dev.kikugie.stonecutter.build

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.ext.ConstantContainer
import dev.kikugie.stonecutter.build.ext.DependencyContainer
import dev.kikugie.stonecutter.build.ext.ReplacementContainer
import dev.kikugie.stonecutter.build.ext.SwapContainer
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.ext.FlagContainer
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import dev.kikugie.stonecutter.util.configure
import groovy.lang.Closure
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.util.internal.ConfigureUtil

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class BuildDsl

/**Stonecutter plugin available in `build.gradle[.kts]`.*/
@StonecutterAPI @BuildDsl
public interface StonecutterBuildExtension : VersionOperations<Version> {
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

    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    public val constants: ConstantContainer
    public val swaps: SwapContainer
    public val dependencies: DependencyContainer
    public val replacements: ReplacementContainer
    public val filters: PatternFilterable
    public val flags: FlagContainer
    public val tasks: StonecutterBuildTasks

    public infix fun constants(action: ConstantContainer.() -> Unit): Unit = constants.action()
    public fun constants(action: Closure<*>): Unit = action.configure(constants)

    public infix fun swaps(action: SwapContainer.() -> Unit): Unit = swaps.action()
    public fun swaps(action: Closure<*>): Unit = action.configure(swaps)

    public infix fun dependencies(action: DependencyContainer.() -> Unit): Unit = dependencies.action()
    public fun dependencies(action: Closure<*>): Unit = action.configure(dependencies)

    public infix fun replacements(action: ReplacementContainer.() -> Unit): Unit = replacements.action()
    public fun replacements(action: Closure<*>): Unit = action.configure(replacements)

    public infix fun flags(action: FlagContainer.() -> Unit): Unit = flags.action()
    public fun flags(action: Closure<*>): Unit = action.configure(flags)

    public infix fun tasks(action: StonecutterBuildTasks.() -> Unit): Unit = tasks.action()
    public fun tasks(action: Closure<*>): Unit = action.configure(tasks)
}