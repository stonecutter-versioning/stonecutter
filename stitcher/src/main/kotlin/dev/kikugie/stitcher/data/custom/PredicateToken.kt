package dev.kikugie.stitcher.data.custom

import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionPredicate
import dev.kikugie.stitcher.data.StitcherToken

internal data class PredicateToken(val predicate: VersionPredicate, override val range: IntRange) : StitcherToken {
    override val text: String get() = "${predicate.operator.literal}${predicate.version.value}"

    infix fun satisfies(version: Version): Boolean = predicate(version)
}
