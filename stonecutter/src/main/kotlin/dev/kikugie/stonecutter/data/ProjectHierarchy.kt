package dev.kikugie.stonecutter.data

import kotlinx.serialization.Serializable
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.invocation.Gradle

/**
 * Represents a validated absolute Gradle project path.
 *
 * The path is colon-separated, with the first character being a colon:
 * - `:`: root project, empty segments.
 * - `:core:subproject`: nested, two segments.
 *
 * The value obtained from [ProjectHierarchy.toString] is equal to [Project.getPath],
 * and can be obtained using [ProjectHierarchy.Companion.hierarchy] extension.
 */
@JvmInline @Serializable
public value class ProjectHierarchy(private val path: String) : Collection<String>, java.io.Serializable {
    init {
        require(path.startsWith(':')) { "Path '$path' must be absolute" }
        require(all(String::isNotBlank)) { "Path '$path' contains invalid segments" }
    }

    /**The number of segments in the path.*/
    override val size: Int get() = if (isEmpty()) 0 else path.count { it == ':' }

    /**The path string.*/
    override fun toString(): String = path

    /**Whenever this is a root project.*/
    override fun isEmpty(): Boolean = path.length == 1
    override fun iterator(): Iterator<String> = path.segments.iterator()
    override fun contains(element: String): Boolean = path.contains(element)
    override fun containsAll(elements: Collection<String>): Boolean = elements.all(path::contains)

    public fun orBlank(): String = if (isEmpty()) "" else path
    public fun relativize(child: ProjectHierarchy): String =
        child.path.removePrefix(path).removePrefix(":")
    public operator fun plus(child: String): ProjectHierarchy = ProjectHierarchy("${orBlank()}:$child")
    public operator fun minus(child: String): ProjectHierarchy {
        var modified = path.removeSuffix(child)
        if (modified.length > 1 && modified.endsWith(':'))
            modified = modified.removeSuffix(":")

        return ProjectHierarchy(modified)
    }

    public companion object {
        public val ROOT: ProjectHierarchy = ProjectHierarchy(":")

        /**Converts the project path to [ProjectHierarchy].*/
        public val Project.hierarchy: ProjectHierarchy get() = ProjectHierarchy(path)
        public val ProjectDescriptor.hierarchy: ProjectHierarchy get() = ProjectHierarchy(path)

        /**Locates the project with the given [hierarchy] in the build.*/
        public fun Project.locate(hierarchy: ProjectHierarchy): Project = project(hierarchy.path)

        /**Locates the project with the given [hierarchy] in the build.*/
        public fun Gradle.locate(hierarchy: ProjectHierarchy): Project = rootProject.locate(hierarchy)

        /**Creates a new [ProjectHierarchy], prepending `:` if necessary.*/
        public fun of(path: String): ProjectHierarchy = ProjectHierarchy(":${path.trimStart(':')}")
        public fun of(components: Iterable<String>): ProjectHierarchy = ProjectHierarchy(components.joinToString(":", ":"))
        public fun of(vararg components: String): ProjectHierarchy = of(components.asIterable())
    }
}

private val String.segments: Sequence<String> get() = substring(1).splitToSequence(':')