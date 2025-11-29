package dev.kikugie.stonecutter.data.container

import dev.kikugie.stonecutter.data.ProjectHierarchy
import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

/**Represents a Gradle-level extension used for storing per-project data.*/
internal abstract class GradleContainerExtension<T : Any>(private val projects: MutableMap<ProjectHierarchy, T> = mutableMapOf()) {
    operator fun contains(hierarchy: ProjectHierarchy): Boolean = hierarchy in projects
    operator fun get(hierarchy: ProjectHierarchy): T? = projects[hierarchy]
    operator fun set(hierarchy: ProjectHierarchy, value: T) {
        check(hierarchy !in projects) { "Project $hierarchy is already registered" }
        projects[hierarchy] = value
    }

    companion object {
        inline fun <reified T : Any> Gradle.createContainer(): T =
            extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" })

        inline fun <reified T : Any> Gradle.createContainer(vararg args: Any): T =
            extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" }, *args)

        inline fun <reified T : Any> Gradle.getContainer(): T =
            extensions.getByType<T>()
    }
}