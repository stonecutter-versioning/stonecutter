package dev.kikugie.stonecutter.controller.tree

import dev.kikugie.stonecutter.data.container.GradleContainerExtension
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree

internal abstract class ProjectNodeContainer : GradleContainerExtension<ProjectNode>() {
    operator fun plusAssign(tree: ProjectTree) {
        for (node in tree.nodes) this[node.hierarchy] = node
    }
}