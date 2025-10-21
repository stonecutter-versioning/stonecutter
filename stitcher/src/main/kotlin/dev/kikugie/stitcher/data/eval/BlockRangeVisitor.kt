package dev.kikugie.stitcher.data.eval

import dev.kikugie.stitcher.data.composite.BlockToken
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.CommentBlock
import dev.kikugie.stitcher.data.composite.ContentBlock
import dev.kikugie.stitcher.data.composite.RootBlock

internal object BlockStartVisitor : BlockToken.Visitor<Int> {
    override fun visitContent(content: ContentBlock): Int = content.leaf.range.first
    override fun visitComment(comment: CommentBlock): Int = comment.opener.range.first
    override fun visitCode(code: CodeBlock): Int = code.host.opener.range.first
    override fun visitRoot(root: RootBlock): Int = root.scope.first().accept(this)

    fun BlockToken.start(): Int = accept(BlockStartVisitor)
    fun List<BlockToken>.start(): Int = first().accept(BlockStartVisitor)
}

internal object BlockStopVisitor : BlockToken.Visitor<Int> {
    override fun visitContent(content: ContentBlock): Int = content.leaf.range.last
    override fun visitComment(comment: CommentBlock): Int = comment.closer.range.last
    override fun visitCode(code: CodeBlock): Int = code.scope.lastOrNull()?.accept(this) ?: code.host.closer.range.last
    override fun visitRoot(root: RootBlock): Int = root.scope.last().accept(this)

    fun BlockToken.stop(): Int = accept(BlockStopVisitor)
    fun List<BlockToken>.stop(): Int = last().accept(BlockStopVisitor)
}

internal object BlockRangeVisitor {
    fun BlockToken.range() = accept(BlockStartVisitor)..accept(BlockStopVisitor)
    fun List<BlockToken>.range() = first().accept(BlockStartVisitor)..last().accept(BlockStopVisitor)
}