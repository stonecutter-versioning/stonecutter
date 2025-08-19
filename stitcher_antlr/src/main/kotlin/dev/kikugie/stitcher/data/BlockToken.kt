package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.util.merge
import org.antlr.v4.runtime.CharStream

internal sealed interface BlockToken : StitcherToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitContent(it: Content): T
        fun visitComment(it: Comment): T
        fun visitCode(it: Code): T
        fun visitRoot(it: Root): T
    }

    data class Content(override val range: IntRange, override val source: CharStream) : BlockToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitContent(this)
    }

    data class Comment(val body: Content, override val range: IntRange) : BlockToken {
        val opener: IntRange get() = range.first..<body.range.first
        val closer: IntRange get() = body.range.last + 1..range.last
        override val source: CharStream get() = body.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitComment(this)
    }

    // FIXME: add a proper range
    data class Code(val marker: LeafToken, val definition: DefinitionToken, val scope: List<BlockToken> = emptyList()) : BlockToken {
        override val range: IntRange get() = merge(marker.range, definition.range, scope.lastOrNull()?.range)
        override val source: CharStream get() = marker.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCode(this)
    }

    data class Root(var scope: List<BlockToken>) : BlockToken {
        override val range: IntRange get() = merge(scope.first().range, scope.last().range)
        override val source: CharStream get() = scope.first().source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitRoot(this)
    }
}