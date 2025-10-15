package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.antlr.StitcherParser

internal data class LeafToken(val type: Type, override val range: IntRange, override val text: String) : StitcherToken {
    fun <T> accept(visitor: NodeTokenVisitor<T>): T = visitor.visitLeaf(this)

    @JvmInline value class Type(val value: Int) {
        val name: String get() = StitcherParser.VOCABULARY.getDisplayName(value)
    }
}