package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.locate
import dev.kikugie.stonecutter.data.StonecutterProject
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import java.nio.file.Path

internal class ProjectNodeImpl(
    override val hierarchy: ProjectHierarchy,
    override val location: Path,
    override val metadata: StonecutterProject,
) : ProjectNode {
    constructor(gradle: Gradle, hierarchy: ProjectHierarchy, metadata: StonecutterProject) :
        this(hierarchy, gradle.locate(hierarchy).projectDir.toPath(), metadata)

    internal val gradle: Gradle get() = (branch as ProjectBranchImpl).gradle
    override val project: Project get() = gradle.locate(hierarchy)
    override lateinit var branch: ProjectBranch
}

internal class ProjectBranchImpl(
    override val hierarchy: ProjectHierarchy,
    override val location: Path,
    override val id: Identifier,
    delegate: Map<Identifier, ProjectNode>
) : ProjectBranch, Map<Identifier, ProjectNode> by delegate {
    constructor(gradle: Gradle, hierarchy: ProjectHierarchy, id: Identifier, nodes: Collection<ProjectNode>) :
        this(hierarchy, gradle.locate(hierarchy).projectDir.toPath(), id, nodes.associateBy { it.metadata.project })

    internal val gradle: Gradle get() = (tree as ProjectTreeImpl).gradle
    override val project: Project get() = gradle.locate(hierarchy)
    override val nodes: Collection<ProjectNode> get() = values
    override val versions: Collection<StonecutterProject> by lazy { values.map(ProjectNode::metadata) }
    override lateinit var tree: ProjectTree

    override fun get(node: ProjectHierarchy): ProjectNode? =
        get(hierarchy.relativize(node))
}

internal class ProjectTreeImpl(
    override val hierarchy: ProjectHierarchy,
    override val location: Path,
    override val vcs: StonecutterProject,
    delegate: Map<Identifier, ProjectBranch>
) : ProjectTree, Map<Identifier, ProjectBranch> by delegate {
    constructor(gradle: Gradle, hierarchy: ProjectHierarchy, vcs: StonecutterProject, branches: Collection<ProjectBranch>) :
        this(hierarchy, gradle.locate(hierarchy).projectDir.toPath(), vcs, branches.associateBy(ProjectBranch::id)) {
        this.gradle = gradle
    }

    @Transient
    internal lateinit var gradle: Gradle

    override var current: StonecutterProject? = null
        internal set(value) {
            if (value == field) return
            for (it in nodes)
                it.metadata.isActive = it.metadata == value
            field = value
        }

    override val project: Project get() = gradle.locate(hierarchy)
    override val branches: Collection<ProjectBranch> get() = values
    override val nodes: Collection<ProjectNode> by lazy { values.flatMap(ProjectBranch::nodes) }
    override val versions: Set<StonecutterProject> by lazy { values.flatMap(ProjectBranch::versions).toSet() }

    override fun get(branch: ProjectHierarchy): ProjectBranch? =
        get(hierarchy.relativize(branch))

    override fun node(node: ProjectHierarchy): ProjectNode? {
        val (branch, node) = hierarchy.relativize(node).split(':', limit = 2)
        return get(branch)?.get(node)
    }
}