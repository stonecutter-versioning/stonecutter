package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.antlr.StitcherParser
import org.antlr.v4.runtime.CharStream

internal sealed interface LeafToken : StitcherToken {
    val type: Int
    val name: String
        get() = StitcherParser.VOCABULARY.getDisplayName(type)

    fun <T> accept(visitor: NodeTokenVisitor<T>): T = visitor.visitLeaf(this)
    data class Sourced(override val type: Int, override val range: IntRange, override val source: CharStream) : LeafToken
    data class Injected(override val type: Int, override val range: IntRange, override val source: CharStream, override val text: String) : LeafToken
}