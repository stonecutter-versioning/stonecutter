package dev.kikugie.stitcher.data

import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import org.antlr.v4.runtime.CharStream

internal data class PredicateToken(val comparator: VersionOperator, val version: Version, val token: LeafToken) : StitcherToken by token {
    fun <T> accept(visitor: NodeTokenVisitor<T>): T = visitor.visitPredicate(this)
}