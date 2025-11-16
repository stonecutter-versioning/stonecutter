package dev.kikugie.stitcher.data.eval

import dev.kikugie.stitcher.data.composite.BlockToken
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.CommentBlock
import dev.kikugie.stitcher.data.composite.ContentBlock
import dev.kikugie.stitcher.data.composite.RootBlock

internal object BlockIsBlankVisitor : BlockToken.Visitor<Boolean> {
    override fun visitContent(content: ContentBlock): Boolean = content.leaf.text.isBlank()
    override fun visitComment(comment: CommentBlock): Boolean = comment.body.text.isBlank()
    override fun visitCode(code: CodeBlock): Boolean = false
    override fun visitRoot(root: RootBlock): Boolean = root.scope.all { it.accept(this) }

    fun BlockToken.isBlank(): Boolean = accept(this@BlockIsBlankVisitor)
    fun BlockToken.isNotBlank(): Boolean = !accept(this@BlockIsBlankVisitor)
}