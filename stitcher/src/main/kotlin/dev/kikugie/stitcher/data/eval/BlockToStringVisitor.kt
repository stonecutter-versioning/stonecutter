package dev.kikugie.stitcher.data.eval

import dev.kikugie.stitcher.data.composite.BlockToken
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.CommentBlock
import dev.kikugie.stitcher.data.composite.ContentBlock
import dev.kikugie.stitcher.data.composite.RootBlock
import dev.kikugie.stitcher.data.leaf.LeafToken

@Suppress("NOTHING_TO_INLINE")
private inline fun StringBuilder.accept(leaf: LeafToken) = append(leaf.text)

internal class BlockToStringVisitor private constructor(private val builder: StringBuilder) : BlockToken.Visitor<Any?> {
    override fun visitContent(content: ContentBlock) = builder.accept(content.leaf)

    override fun visitComment(comment: CommentBlock) = with(builder) {
        comment.opener?.let(::accept)
        accept(comment.body)
        comment.closer?.let(::accept)
    }

    override fun visitCode(code: CodeBlock) {
        code.host.accept(this)
        for (it in code.scope) it.accept(this)
    }

    override fun visitRoot(root: RootBlock) {
        for (it in root.scope) it.accept(this)
    }

    companion object {
        fun BlockToken.join(): String = buildString {
            accept((BlockToStringVisitor(this)))
        }
        fun BlockToken.join(builder: StringBuilder): StringBuilder = builder.also {
            accept(BlockToStringVisitor(it))
        }
        fun List<BlockToken>.join(): String = buildString {
            for (it in this@join) it.join(this)
        }
    }
}