@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package dev.kikugie.stitcher.data.composite

import dev.kikugie.stitcher.data.leaf.LeafToken

internal sealed interface BlockToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitContent(content: ContentBlock): T
        fun visitComment(comment: CommentBlock): T
        fun visitCode(code: CodeBlock): T
        fun visitRoot(root: RootBlock): T
    }
}

internal data class ContentBlock(val leaf: LeafToken) : BlockToken {
    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitContent(this)
}

internal data class CommentBlock(val opener: LeafToken?, val body: LeafToken, val closer: LeafToken?) : BlockToken {
    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitComment(this)
}

internal data class CodeBlock(
    val host: CommentBlock,
    val marker: LeafToken,
    val definition: DefinitionToken,
    val scope: List<BlockToken> = emptyList()
) : BlockToken {
    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitCode(this)
}

internal data class RootBlock(val scope: List<BlockToken>) : BlockToken {
    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitRoot(this)
}