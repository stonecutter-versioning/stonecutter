package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.data.version.LenientOperations
import dev.kikugie.semver.data.Version
import kotlin.compareTo

/**
 * Parsed representation of [StonecutterProject.version], providing additional comparison utilities.
 *
 * @property value The underlying parsed version
 */
@JvmInline @JvmExposeBoxed @OptIn(ExperimentalStdlibApi::class)
public value class ParsedVersion(public val value: Version) : Comparable<Any> {
    /**Creates a parsed version from a string [value]. Throws if the version is invalid.*/
    public constructor(value: AnyVersion) : this(Version.parse(value).getOrThrow())

    /**
     * Compares this version to the [other].
     *
     * The allowed values are:
     * - [ParsedVersion]
     * - [Version]
     * - [String]
     */
    override fun compareTo(other: Any): Int = when (other) {
        is ParsedVersion -> value compareTo other.value
        is Version -> value compareTo other
        is String -> value compareTo LenientOperations.parse(other)
        else -> throw IllegalArgumentException("Unsupported comparison target of ${other::class.qualifiedName}")
    }

    /**
     * Checks if this version is loosely equal to the [other].
     *
     * Accepts the same types as [compareTo], and ignored SemVer build metadata.
     */
    public infix fun eq(other: Any): Boolean =
        compareTo(other) == 0

    /**
     * Checks if this version matches the space-separated [predicates].
     */
    public infix fun matches(predicates: String): Boolean =
        LenientOperations.eval(value, predicates)
}