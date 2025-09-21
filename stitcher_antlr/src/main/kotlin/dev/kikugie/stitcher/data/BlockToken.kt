package dev.kikugie.stitcher.data

internal sealed interface BlockToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitContent(it: Content): T
        fun visitComment(it: Comment): T
        fun visitCode(it: Code): T
        fun visitRoot(it: Root): T
    }

    data class Content(val leaf: LeafToken) : BlockToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitContent(this)
    }

    data class Comment(val opener: LeafToken, val body: LeafToken, val closer: LeafToken) : BlockToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitComment(this)
    }

    data class Code(
        val opener: LeafToken,
        val marker: LeafToken,
        val definition: DefinitionToken,
        val closer: LeafToken,
        val scope: List<BlockToken> = emptyList()
    ) : BlockToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCode(this)
    }

    data class Root(var scope: List<BlockToken>) : BlockToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitRoot(this)
    }
}