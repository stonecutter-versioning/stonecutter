package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.antlr.StitcherParser
import org.antlr.v4.runtime.CharStream

internal data class LeafToken(val type: Int, override val range: IntRange, override val source: CharStream) : StitcherToken {
    val typeName: String get() = StitcherParser.VOCABULARY.getDisplayName(type)
    fun <T> accept(visitor: NodeTokenVisitor<T>): T = visitor.visitLeaf(this)
}