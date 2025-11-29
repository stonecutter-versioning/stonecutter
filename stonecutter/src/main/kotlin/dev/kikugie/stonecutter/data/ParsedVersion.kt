package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.data.version.LenientOperations
import dev.kikugie.semver.data.Version
import kotlin.compareTo

@JvmInline
public value class ParsedVersion(public val value: Version) : Comparable<Any> {
    public constructor(value: AnyVersion) : this(Version.parse(value).getOrThrow())

    override fun compareTo(other: Any): Int = when (other) {
        is ParsedVersion -> value compareTo other.value
        is Version -> value compareTo other
        is String -> value compareTo LenientOperations.parse(other)
        else -> throw IllegalArgumentException("Unsupported comparison target of ${other::class.qualifiedName}")
    }

    public infix fun eq(other: Any): Boolean =
        compareTo(other) == 0

    public infix fun matches(predicate: String): Boolean =
        LenientOperations.eval(value, predicate)
}