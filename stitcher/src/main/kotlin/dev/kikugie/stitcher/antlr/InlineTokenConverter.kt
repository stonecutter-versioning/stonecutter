package dev.kikugie.stitcher.antlr

import dev.kikugie.commons.ranges.shr
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.merge
import dev.kikugie.stitcher.util.range
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

internal class InlineTokenConverter(val start: Int) {
    operator fun invoke(type: Int, range: IntRange, text: String): LeafToken = LeafToken(type, range shr start, text)

    operator fun invoke(type: Int, ctx: ParserRuleContext) = invoke(type, ctx.range, ctx.text)
    operator fun invoke(token: AntlrToken): LeafToken = invoke(token.type, token.range, token.text)
    operator fun invoke(node: TerminalNode): LeafToken = invoke(node.symbol)
    operator fun invoke(tokens: List<AntlrToken>): LeafToken =
        invoke(tokens.first().type, merge(tokens.first().range, tokens.last().range), tokens.joinToString("", transform = AntlrToken::getText))

    companion object {
        val DEFAULT = InlineTokenConverter(0)
    }
}