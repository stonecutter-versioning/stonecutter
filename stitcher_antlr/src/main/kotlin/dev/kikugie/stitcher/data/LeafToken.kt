package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.antlr.StitcherParser

internal data class LeafToken(val type: Int, override val range: IntRange, override val text: String) : StitcherToken {
    val name: String get() = StitcherParser.VOCABULARY.getDisplayName(type)
    fun <T> accept(visitor: NodeTokenVisitor<T>): T = visitor.visitLeaf(this)
}