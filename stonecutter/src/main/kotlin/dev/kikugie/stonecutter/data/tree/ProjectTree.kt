package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import org.gradle.api.Project
import java.nio.file.Path

/**
 * Represents a member entity within a Gradle-based structure that provides access to its [hierarchy],
 * the [location] it resides in, and the associated Gradle [project].
 */
public interface GradleMember {
    /**
     * The [directory][Path] where the associated member resides within the Gradle project structure.
     */
    public val location: Path

    /**
     * The `:`-separated absolute [Gradle path][ProjectHierarchy] of the associated member.
     */
    public val hierarchy: ProjectHierarchy

    /**
     * The associated Gradle [Project] instance representing the current context.
     * This property **is not available** at task runtime.
     */
    @get:Throws
    public val project: Project
}

/**
 * Represents a node in a project structure associated with a [StonecutterProject] and a [branch].
 * Provides methods to navigate between related nodes within the structure.
 */
public interface ProjectNode : GradleMember {
    /**
     * Contains the project name, assigned version, and the active status, represented by a [StonecutterProject].
     */
    public val metadata: StonecutterProject

    /**
     * The [ProjectBranch] this node belongs to.
     */
    public val branch: ProjectBranch

    /**
     * The [ProjectTree] this node belongs to.
     */
    public val tree: ProjectTree
        get() = branch.tree

    /**
     * Retrieves a related project node within the same [branch] based on the specified [node] identifier.
     */
    public fun peer(node: Identifier): ProjectNode? = find(branch.id, node)

    /**
     * Retrieves a sibling [ProjectNode] within the same project hierarchy in the specified [branch].
     */
    public fun sibling(branch: Identifier): ProjectNode? = find(branch, metadata.project)

    /**
     * Retrieves a [ProjectNode] within the specified [branch] of the project hierarchy identified by the [node].
     */
    public fun find(branch: Identifier, node: Identifier): ProjectNode? = this.branch.tree.get(branch)?.get(node)
}

/**
 * Represents a branch in the [ProjectTree].
 * Implements [GradleMember] and maps [ProjectNode.metadata.project][StonecutterProject.project] keys to [ProjectNode] values.
 */
public interface ProjectBranch : GradleMember, Map<Identifier, ProjectNode> {
    /**
     * The name of this branch.
     * The root branch has a reserved name of an empty string.
     */
    public val id: Identifier

    /**
     * The [ProjectTree] this branch belongs to.
     */
    public val tree: ProjectTree

    /**
     * All the project nodes in this branch. Equivalent to using [values].
     */
    public val nodes: Collection<ProjectNode>

    /**
     * All [ProjectNode.metadata] entries of the nodes.
     */
    public val versions: Collection<StonecutterProject>

    /**
     * Retrieves a [ProjectNode] associated with the given [node] in the project hierarchy.
     */
    public operator fun get(node: ProjectHierarchy): ProjectNode?
}

/**
 * Represents a hierarchical structure of projects, offering access to branches, nodes, and version control information.
 * Implements [GradleMember] and maps [ProjectBranch.id] keys to [ProjectBranch] values.
 */
public interface ProjectTree : GradleMember, Map<Identifier, ProjectBranch> {
    /**
     * The version control reset point for this tree.
     */
    public val vcs: StonecutterProject

    /**
     * The active version or `null` if the project is not using an active version.
     * *(You **really** need to know what you're doing)*
     */
    public val current: StonecutterProject?

    /**
     * All the branches in this tree. Equivalent to using [values]
     */
    public val branches: Collection<ProjectBranch>

    /**
     * All the nodes.
     */
    public val nodes: Collection<ProjectNode>

    /**
     * All **unique** [ProjectNode.metadata] entries.
     */
    public val versions: Set<StonecutterProject>

    /**
     * Retrieves the [ProjectNode] associated with the provided [node].
     */
    public fun node(node: ProjectHierarchy): ProjectNode?

    /**
     * Retrieves the [ProjectBranch] associated with the given [branch].
     */
    public operator fun get(branch: ProjectHierarchy): ProjectBranch?
}