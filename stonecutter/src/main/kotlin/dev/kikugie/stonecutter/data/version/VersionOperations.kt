package dev.kikugie.stonecutter.data.version

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.commons.takeAs
import dev.kikugie.commons.text.countMatching
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionPredicate
import java.util.concurrent.ConcurrentHashMap

/**Marker interface for extensions supporting version-based conditional evaluation.*/
public interface VersionOperations<T : Version> {
    /**Parses the provided [version] string as the [T] version.*/
    public fun parse(version: AnyVersion): T

    /**
     * Checks is the provided [version] satisfies the [predicates].
     *
     * Each predicate string may contain multiple space-separated entries.
     */
    public fun eval(version: Version, vararg predicates: String): Boolean

    /**
     * Checks is the provided [version] satisfies the [predicates].
     *
     * Each predicate string may contain multiple space-separated entries.
     */
    public fun eval(version: String, vararg predicates: String): Boolean =
        eval(parse(version), *predicates)

    /**Parses both version strings and compares them.*/
    public fun compare(version: String, other: String): Int =
        parse(version) compareTo parse(other)
}

internal object LenientOperations : VersionOperations<Version> {
    private val CACHE: MutableMap<String, Version> = ConcurrentHashMap()
    override fun parse(version: AnyVersion): Version =
        CACHE.getOrParse(version, Version).takeAs()

    override fun eval(version: Version, vararg predicates: String): Boolean =
        predicates.unpack(VersionPredicate).all { it(version) }
}

internal object SemanticOperations : VersionOperations<SemanticVersion> {
    private val CACHE: MutableMap<String, Version> = ConcurrentHashMap()
    override fun parse(version: AnyVersion): SemanticVersion =
        CACHE.getOrParse(version, SemanticVersion).takeAs()

    override fun eval(version: Version, vararg predicates: String): Boolean =
        predicates.unpack(VersionPredicate.Semantic).all { it(version) }
}

private fun MutableMap<AnyVersion, Version>.getOrParse(version: AnyVersion, ops: Version.Operations): Version =
    computeIfAbsent(version, ops::parse)

private fun Array<out String>.unpack(ops: VersionPredicate.Operations) = buildList {
    for (it in this@unpack) unpackPredicates(it, ops)
}

private fun MutableList<VersionPredicate>.unpackPredicates(source: String, ops: VersionPredicate.Operations) {
    var cursor = 0
    while (cursor < source.length) {
        cursor += source.countMatching(start = cursor, end = source.length, ' ', '\t')
        if (cursor >= source.length) break else cursor = locateAndParse(source, cursor, ops)
    }
}

private fun MutableList<VersionPredicate>.locateAndParse(source: String, cursor: Int, ops: VersionPredicate.Operations): Int {
    val end = ops.locate(source, cursor)
    require(end >= 0) { "Unable to locate predicate at: '${source.substring(cursor)}'" }

    val string = source.substring(cursor, end)
    val predicate = ops.parse(string).getOrElse {
        throw IllegalArgumentException("Invalid predicate: '$string'").initCause(it)
    }

    this += predicate
    return end
}