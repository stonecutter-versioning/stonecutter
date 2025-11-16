@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package dev.kikugie.stitcher.data.composite

import dev.kikugie.stitcher.data.eval.BlockFirstChildVisitor.firstChild
import dev.kikugie.stitcher.data.eval.BlockLastChildVisitor.lastChild
import dev.kikugie.stitcher.data.leaf.LeafToken

internal sealed interface BlockToken {
    // FIXME: not set yet
    var parent: BlockToken?
    var nextBlock: BlockToken?
    var prevBlock: BlockToken?

    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitContent(content: ContentBlock): T
        fun visitComment(comment: CommentBlock): T
        fun visitCode(code: CodeBlock): T
        fun visitRoot(root: RootBlock): T
    }

    companion object {
        fun link(left: BlockToken, right: BlockToken) {
            left.nextBlock = right
            right.prevBlock = left
        }

        fun link(tokens: Iterable<BlockToken>) =
            link(tokens.asSequence())
        fun link(tokens: Sequence<BlockToken>) {
            for ((a, b) in tokens.zipWithNext())
                link(a, b)
        }
    }
}

internal data class ContentBlock(val leaf: LeafToken) : BlockToken {
    override var parent: BlockToken? = null
        set(value) { require(value !is ContentBlock && value !is CommentBlock) { "Parent must be a container" }; field = value }
    override var nextBlock: BlockToken? = null
    override var prevBlock: BlockToken? = null

    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitContent(this)
}

internal data class CommentBlock(val opener: LeafToken?, val body: LeafToken, val closer: LeafToken?) : BlockToken {
    override var parent: BlockToken? = null
        set(value) { require(value !is ContentBlock && value !is CommentBlock) { "Parent must be a container" }; field = value }
    override var nextBlock: BlockToken? = null
    override var prevBlock: BlockToken? = null

    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitComment(this)
}

internal data class CodeBlock(
    val host: CommentBlock,
    val marker: LeafToken,
    val definition: DefinitionToken,
    val scope: List<BlockToken> = emptyList()
) : BlockToken {
    override var parent: BlockToken? = null
        set(value) { require(value !is ContentBlock && value !is CommentBlock) { "Parent must be a container" }; field = value }
    override var nextBlock: BlockToken?
        get() = host
        set(value) { host.nextBlock = value?.firstChild }
    override var prevBlock: BlockToken?
        get() = host.prevBlock
        set(value) { host.prevBlock = value?.lastChild }

    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitCode(this)
}

internal data class RootBlock(val scope: List<BlockToken>) : BlockToken {
    override var parent: BlockToken? = null
        set(value) { require(value !is ContentBlock && value !is CommentBlock) { "Parent must be a container" }; field = value }
    override var nextBlock: BlockToken?
        get() = firstChild.nextBlock
        set(value) { firstChild.nextBlock = value?.firstChild }
    override var prevBlock: BlockToken?
        get() = firstChild.prevBlock
        set(value) { firstChild.prevBlock = value?.lastChild }

    override fun <T> accept(visitor: BlockToken.Visitor<T>): T = visitor.visitRoot(this)
}