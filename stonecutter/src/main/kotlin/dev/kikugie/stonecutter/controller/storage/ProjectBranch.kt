package dev.kikugie.stonecutter.controller.storage

import dev.kikugie.stonecutter.ProjectName
import dev.kikugie.stonecutter.StonecutterProject
import org.gradle.api.Project
import java.nio.file.Path

/**
 * Branch in a [ProjectTree], containing registered nodes.
 *
 * @property project Gradle project of this branch.
 * **May be the same as [ProjectTree.project] if this is the root branch**
 * @property id Name of this branch. Same as the key in [ProjectTree.branches].
 * **Empty string for the root branch**
 * @property nodes Nodes accessible by their project names
 */
data class ProjectBranch(
    private val project: Project,
    val id: ProjectName,
    private val _nodes: Map<ProjectName, ProjectNode>,
): Map<ProjectName, ProjectNode> by _nodes, Project by project {
    internal companion object {
        operator fun ProjectBranch?.get(project: ProjectName) = this?.get(project)
    }

    /**
     * Location of this branch on the disk.
     */
    val path: Path = projectDir.toPath()

    /**
     * All nodes in this branch.
     */
    val nodes: Collection<ProjectNode> = values

    /**
     * [StonecutterProject] instances of all [nodes].
     */
    val versions: Collection<StonecutterProject> = values.map { it.metadata }

    /**
     * Reference to the tree containing this branch.
     */
    lateinit var tree: ProjectTree
        internal set

    init {
        values.forEach { it.branch = this }
    }

    /**
     * Finds an entry for the given project.
     *
     * @param project Project reference
     */
    operator fun get(project: Project) = get(project.path.substringAfterLast(':'))
}