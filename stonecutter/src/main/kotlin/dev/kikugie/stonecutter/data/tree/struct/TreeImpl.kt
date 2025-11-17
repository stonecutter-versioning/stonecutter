package dev.kikugie.stonecutter.data.tree.struct

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import java.nio.file.Path

private fun Gradle.resolve(project: ProjectHierarchy): Project =
    if (project.isEmpty()) rootProject else rootProject.project(project.toString().removePrefix(":"))

internal class ProjectNodeImpl(
    override val hierarchy: ProjectHierarchy,
    override val location: Path,
    override val metadata: StonecutterProject,
) : ProjectNode {
    constructor(gradle: Gradle, hierarchy: ProjectHierarchy, metadata: StonecutterProject) : this(
        hierarchy, gradle.resolve(hierarchy).projectDir.toPath(), metadata
    )

    internal val gradle: Gradle
        get() = (branch as ProjectBranchImpl).gradle
    override lateinit var branch: ProjectBranch
    override val project: Project
        get() = gradle.resolve(hierarchy)
}

internal class ProjectBranchImpl(
    override val hierarchy: ProjectHierarchy,
    override val location: Path,
    override val id: Identifier,
    delegate: Map<Identifier, ProjectNode>
) : ProjectBranch, Map<Identifier, ProjectNode> by delegate {
    constructor(gradle: Gradle, hierarchy: ProjectHierarchy, id: Identifier, nodes: Collection<ProjectNode>) : this(
        hierarchy, gradle.resolve(hierarchy).projectDir.toPath(), id, nodes.associateBy { it.metadata.project }
    )

    internal val gradle: Gradle
        get() = (tree as ProjectTreeImpl).gradle
    override lateinit var tree: ProjectTree
    override val nodes: Collection<ProjectNode>
        get() = values
    override val versions: Collection<StonecutterProject>
        by lazy { values.map(ProjectNode::metadata) }
    override val project: Project
        get() = gradle.resolve(hierarchy)

    override fun get(node: ProjectHierarchy): ProjectNode? =
        get(hierarchy.relativize(node))
}

@OptIn(StonecutterInternalAPI::class)
internal class ProjectTreeImpl(
    override val hierarchy: ProjectHierarchy,
    override val location: Path,
    override val vcs: StonecutterProject,
    delegate: Map<Identifier, ProjectBranch>
) : ProjectTree, Map<Identifier, ProjectBranch> by delegate {
    constructor(gradle: Gradle, hierarchy: ProjectHierarchy, vcs: StonecutterProject, branches: Collection<ProjectBranch>) : this(
        hierarchy, gradle.resolve(hierarchy).projectDir.toPath(), vcs, branches.associateBy(ProjectBranch::id)
    ) {
        this.gradle = gradle
    }

    @Transient
    internal lateinit var gradle: Gradle
    override var current: StonecutterProject? = null
        internal set(value) {
            if (value == field) return
            for (it in nodes)
                it.metadata.overrideActiveState(it.metadata == value)
            field = value
        }
    override val branches: Collection<ProjectBranch>
        get() = values
    override val nodes: Collection<ProjectNode>
        by lazy { values.flatMap(ProjectBranch::nodes) }
    override val versions: Collection<StonecutterProject>
        by lazy { values.flatMap(ProjectBranch::versions).toSet() }
    override val project: Project
        get() = gradle.resolve(hierarchy)

    override fun get(branch: ProjectHierarchy): ProjectBranch? =
        get(hierarchy.relativize(branch))

    override fun node(node: ProjectHierarchy): ProjectNode? {
        val (branch, node) = hierarchy.relativize(node).split(':', limit = 2)
        return get(branch)?.get(node)
    }
}