package dev.kikugie.stitcher.transform.visitor

import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.util.merge

internal object RangeFinder : BlockToken.Visitor<IntRange> {
    override fun visitContent(it: BlockToken.Content): IntRange = it.leaf.range
    override fun visitComment(it: BlockToken.Comment): IntRange = merge(it.opener.range, it.closer.range)
    override fun visitCode(it: BlockToken.Code): IntRange = merge(it.host.opener.range, it.scope.last().accept(this))
    override fun visitRoot(it: BlockToken.Root): IntRange = merge(it.scope.first().accept(this), it.scope.last().accept(this))

    fun BlockToken.range(): IntRange = accept(RangeFinder)
    fun List<BlockToken>.range(): IntRange = merge(first().accept(RangeFinder), last().accept(RangeFinder))
}