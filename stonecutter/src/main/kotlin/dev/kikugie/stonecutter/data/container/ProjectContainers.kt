package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.tree.builder.TreeBuilder
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.create
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance

internal inline fun <reified T : Any> Gradle.createContainer(): T =
    extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" })

internal inline fun <reified T : Any> Gradle.createContainer(vararg args: Any): T =
    extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" }, *args)

internal inline fun <reified T : Any> Gradle.getContainer(): T =
    extensions.getByType<T>()

internal abstract class GradleContainerExtension<T>(val projects: MutableMap<ProjectHierarchy, T> = mutableMapOf()) :
    MutableMap<ProjectHierarchy, T> by projects {
    operator fun get(project: Project): T? = projects[project.hierarchy]
}

internal open class TreeBuilderContainer : GradleContainerExtension<TreeBuilder>()
internal open class ProjectNodeContainer : GradleContainerExtension<ProjectNode>() {
    operator fun plusAssign(tree: ProjectTree) = tree.nodes.forEach { this[it.hierarchy] = it }
}

internal open class BuildPropertiesContainer(val objects: ObjectFactory, val factory: ProviderFactory) {
    private val properties: NamedDomainObjectContainer<StonecutterBuildProperties> =
        objects.domainObjectContainer(StonecutterBuildProperties::class)

    operator fun get(node: ProjectNode): StonecutterBuildProperties = properties.findByName("StonecutterBuild@${node.hierarchy}")
        ?: objects.newInstance<StonecutterBuildProperties>(node, factory).also(properties::add)

    operator fun set(tree: ProjectTree, action: Action<StonecutterBuildExtension>) {
        val nodes = tree.nodes.map { "StonecutterBuild@${it.hierarchy}" }.toSet()
        properties.named { it in nodes }.all(action)
    }
}