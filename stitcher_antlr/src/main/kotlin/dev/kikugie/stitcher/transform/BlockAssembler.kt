package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.acceptThis

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
        it.host.acceptThis()
        for (it in it.scope) it.acceptThis()
    }

    override fun visitRoot(it: BlockToken.Root) {
        for (it in it.scope) it.acceptThis()
    }

    companion object : BlockToken.Visitor<String> {
        override fun visitContent(it: BlockToken.Content): String = buildString { BlockAssembler(this) }
        override fun visitComment(it: BlockToken.Comment): String = buildString { BlockAssembler(this) }
        override fun visitCode(it: BlockToken.Code): String = buildString { BlockAssembler(this) }
        override fun visitRoot(it: BlockToken.Root): String = buildString { BlockAssembler(this) }
    }
}