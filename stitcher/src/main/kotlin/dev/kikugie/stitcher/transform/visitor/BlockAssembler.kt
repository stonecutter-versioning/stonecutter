package dev.kikugie.stitcher.transform.visitor

import dev.kikugie.stitcher.data.BlockToken

internal class BlockAssembler(private val builder: StringBuilder) : BlockToken.Visitor<Unit> {
    override fun visitContent(it: BlockToken.Content) {
        builder.append(it.leaf.text)
    }

    override fun visitComment(it: BlockToken.Comment) {
        builder.append(it.opener.text)
        builder.append(it.body.text)
        builder.append(it.closer.text)
    }

    override fun visitCode(it: BlockToken.Code) {
        it.host.accept(this)
        for (it in it.scope) it.accept(this)
    }

    override fun visitRoot(it: BlockToken.Root) {
        for (it in it.scope) it.accept(this)
    }

    companion object {
        fun BlockToken.join(): String = buildString { accept(BlockAssembler(this)) }
        fun BlockToken.join(builder: StringBuilder): Unit = accept(BlockAssembler(builder))
    }
}