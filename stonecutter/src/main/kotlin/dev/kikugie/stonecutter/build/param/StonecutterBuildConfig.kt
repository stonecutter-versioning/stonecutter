package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranch
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.util.PatternFilterable
import dev.kikugie.semver.data.Version as ParsedVersion

@StonecutterAPI
public interface StonecutterBuildConfig : ExtensionAware, VersionOperations<ParsedVersion> {
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
    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations
}

