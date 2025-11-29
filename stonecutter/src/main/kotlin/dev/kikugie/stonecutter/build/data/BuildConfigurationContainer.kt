package dev.kikugie.stonecutter.build.data

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

internal abstract class BuildConfigurationContainer @Inject constructor(val providers: ProviderFactory, val objects: ObjectFactory) {
    abstract val entries: NamedDomainObjectContainer<StonecutterBuildConfiguration>

    operator fun get(node: ProjectNode): StonecutterBuildConfiguration = synchronized(entries) {
        val name = "StonecutterBuild@${node.hierarchy}"
        entries.findByName(name) ?: objects.newInstance<StonecutterBuildConfiguration>(name, node, providers).also(entries::add)
    }

    operator fun set(tree: ProjectTree, action: Action<StonecutterBuildExtension>) = synchronized(entries) {
        val nodes = tree.nodes.map { "StonecutterBuild@${it.hierarchy}" }.toSet()
        entries.named { it in nodes }.all(action)
    }
}