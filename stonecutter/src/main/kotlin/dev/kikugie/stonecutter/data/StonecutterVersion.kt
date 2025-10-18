@file:Suppress("EqualsOrHashCode")

package dev.kikugie.stonecutter.data

import dev.kikugie.semver.data.Version
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations

/**
 * A wrapper around [Version] adding utilities without requiring extension function imports.
 * The underlying version is accessible with the [value] field.
 */
public class StonecutterVersion(public val value: Version) : Comparable<Any> {
    /**
     * Creates a new [StonecutterVersion] by parsing the [value] either as a
     * [SemanticVersion][dev.kikugie.semver.data.SemanticVersion] or a
     * [StringVersion][dev.kikugie.semver.data.StringVersion].
     * @throws IllegalArgumentException If both conversions fail.
     */
    public constructor(value: String) : this(LenientOperations.parse(value))

    /**
     * Checks if this [value] satisfies the given predicate.
     *
     * The predicate may include multiple entries separated with a space:
     * ```kt
     * val version = StonecutterVersion("1.21.1")
     * assert(version matches ">=1.21 <1.21.5")
     * ```
     *
     * @throws IllegalArgumentException If predicate parsing fails.
     */
    public infix fun matches(predicate: String): Boolean =
        LenientOperations.eval(value, predicate)

    /**
     * Compares this [value] against a [String], a parsed [Version], or a boxed [StonecutterVersion].
     * ```kt
     * // Work the same
     * val version = StonecutterVersion("1.21.1")
     * assert(version >= "1.20")
     * assert(version >= SemanticVersion(intArrayOf(1, 20)))
     * assert(version >= StonecutterVersion("1.20"))
     * ```
     *
     * @throws IllegalArgumentException If [other] is neither of the types listed above.
     */
    override fun compareTo(other: Any): Int = when (other) {
        is StonecutterVersion -> value compareTo other.value
        is Version -> value compareTo other
        is String -> value compareTo LenientOperations.parse(other)
        else -> throw IllegalArgumentException("Unsupported comparison target of ${other::class.qualifiedName}")
    }

    /**
     * Provides **loose** equality for the parsed version.
     *
     * The implementation doesn't follow the object equality specification
     * in favour of syntactic convenience.
     * For **strict** equality, compare the underlying [value].
     * ```kt
     * val version = StonecutterVersion("1.21.1")
     * assert(version == "1.21.1") // Also true!
     * assert(version.value != "1.21.1") // Strict equality
     * ```
     */
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other == null -> false
        else -> try {
            compareTo(other) == 0
        } catch (_: Exception) {
            false
        }
    }
}