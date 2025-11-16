package dev.kikugie.stitcher.data.eval

import dev.kikugie.stitcher.data.composite.BlockToken
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.CommentBlock
import dev.kikugie.stitcher.data.composite.ContentBlock
import dev.kikugie.stitcher.data.composite.RootBlock

internal object BlockFirstChildVisitor : BlockToken.Visitor<BlockToken> {
    override fun visitContent(content: ContentBlock): BlockToken = content
    override fun visitComment(comment: CommentBlock): BlockToken = comment
    override fun visitCode(code: CodeBlock): BlockToken = code.host
    override fun visitRoot(root: RootBlock): BlockToken = root.scope.first()

    val BlockToken.firstChild: BlockToken get() = accept(this@BlockFirstChildVisitor)
}


internal object BlockLastChildVisitor : BlockToken.Visitor<BlockToken> {
    override fun visitContent(content: ContentBlock): BlockToken = content
    override fun visitComment(comment: CommentBlock): BlockToken = comment
    override fun visitCode(code: CodeBlock): BlockToken = code.scope.lastOrNull() ?: code.host
    override fun visitRoot(root: RootBlock): BlockToken = root.scope.last()

    val BlockToken.lastChild: BlockToken get() = accept(this@BlockLastChildVisitor)
}