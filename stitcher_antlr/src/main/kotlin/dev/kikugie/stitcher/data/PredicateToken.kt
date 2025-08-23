package dev.kikugie.stitcher.data

import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import org.antlr.v4.runtime.CharStream

internal data class PredicateToken(
    val comparator: VersionOperator, val version: Version,
    override val range: IntRange, override val source: CharStream
) : StitcherToken {
    fun <T> accept(visitor: NodeTokenVisitor<T>): T = visitor.visitPredicate(this)
}