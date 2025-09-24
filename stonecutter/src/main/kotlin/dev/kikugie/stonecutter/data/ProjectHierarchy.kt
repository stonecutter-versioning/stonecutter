package dev.kikugie.stonecutter.data

import dev.kikugie.commons.then
import dev.kikugie.stonecutter.StonecutterAPI
import kotlinx.serialization.Serializable
import org.gradle.api.Project
import org.gradle.api.UnknownProjectException
import org.gradle.api.initialization.ProjectDescriptor

/**
 * Represents a Gradle project path in colon-separated notation.
 * Provided path must be absolute, starting with `:`.
 * @property path String representation of the path
 */
@StonecutterAPI @JvmInline @Serializable
public value class ProjectHierarchy(private val path: String) : List<String> {
    init {
        require(path.isNotBlank()) { "Path cannot be blank" }
        require(path.startsWith(':')) { "Path must be absolute" }
    }

    /**All subprojects in this path. If this is the root project, the list will be empty.*/
    public val segments: Sequence<String>
        get() = if (path == ":") emptySequence()
        else path.substring(1).splitToSequence(':')

    override val size: Int
        get() = path.count { it == ':' }

    public fun orBlank(): String = if (isEmpty()) "" else path

    public fun relativize(child: ProjectHierarchy): String =
        child.path.removePrefix(path).removePrefix(":")

    /**Creates a new path, with the [child] attached. The [child] property must not start with `:`.*/
    public operator fun plus(child: String): ProjectHierarchy = require(!child.startsWith(":")) then when (path) {
        ":" -> ProjectHierarchy(":$child")
        else -> when(child) {
            "" -> this
            else -> ProjectHierarchy("$path:$child")}
    }

    /**Creates a new path, without the [child] attached. The [child] property must not start with `:`.*/
    public operator fun minus(child: String): ProjectHierarchy = require(!child.startsWith(":")) then when {
        !path.endsWith(":$child") -> this
        path == ":$child" -> ROOT
        else -> ProjectHierarchy(path.removeSuffix(":$child"))
    }

    /**Represents the class as the underlying [path].*/
    override fun toString(): String = path

    override fun contains(element: String): Boolean = element in path

    override fun containsAll(elements: Collection<String>): Boolean = all { it in path }

    override fun get(index: Int): String =
        segments.elementAt(index)

    override fun indexOf(element: String): Int =
        segments.indexOf(element)

    override fun isEmpty(): Boolean = path == ":"

    override fun iterator(): Iterator<String> =
        segments.iterator()

    override fun lastIndexOf(element: String): Int =
        segments.lastIndexOf(element)

    override fun listIterator(): ListIterator<String> =
        segments.toList().listIterator()

    override fun listIterator(index: Int): ListIterator<String> =
        segments.toList().listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<String> =
        segments.toList().subList(fromIndex, toIndex)

    public companion object {
        /**Empty path.*/
        public val ROOT: ProjectHierarchy = ProjectHierarchy(":")
        /**Converts [Project.getPath] to [ProjectHierarchy].*/
        public val Project.hierarchy: ProjectHierarchy get() = ProjectHierarchy(path)

        public val ProjectDescriptor.hierarchy: ProjectHierarchy get() = ProjectHierarchy(path)

        public fun of(path: String): ProjectHierarchy = ProjectHierarchy(":${path.trimStart(':')}")

        public fun of(components: Iterable<String>): ProjectHierarchy = ProjectHierarchy(buildString {
            for (it in components) append(":$it")
        })

        public fun of(vararg components: String): ProjectHierarchy =
            of(components.asIterable())

        /**
         * Gets the Gradle project for the given [hierarchy].
         * Receiver can be any project, since [ProjectHierarchy] is guaranteed to be an absolute path.
         * @throws UnknownProjectException if the path is invalid
         */
        public fun Project.locate(hierarchy: ProjectHierarchy): Project = project(hierarchy.path)
    }
}

