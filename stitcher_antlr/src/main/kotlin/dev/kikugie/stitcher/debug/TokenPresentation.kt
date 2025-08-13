package dev.kikugie.stitcher.debug

import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.get
import org.antlr.v4.runtime.Vocabulary

typealias NamedPresentation = Pair<String, TokenPresentation>

interface TokenPresentation {
    val type: String
    val range: IntRange

    val value: String get() = ""

    fun <T> accept(visitor: Visitor<T>): T = visitor.visitGeneric(this)

    interface Visitor<T> {
        fun visitLeaf(it: Leaf): T
        fun visitStruct(it: Struct): T

        fun visitGeneric(it: TokenPresentation): T
    }

    data class Leaf(val token: LeafToken, val vocabulary: Vocabulary) : TokenPresentation {
        override val type: String get() = vocabulary.getDisplayName(token.type)
        override val range: IntRange get() = token.range
        override val value: String get() = token.source[token.range]
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitLeaf(this)
    }

    data class Struct(val token: StitcherToken, val children: Sequence<NamedPresentation>) : TokenPresentation {
        override val type: String get() = token::class.simpleName!!
        override val range: IntRange get() = token.range
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitStruct(this)
    }
}