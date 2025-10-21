package dev.kikugie.stitcher.parser

import dev.kikugie.commons.ranges.shl
import dev.kikugie.stitcher.data.leaf.LeafToken
import dev.kikugie.stitcher.data.leaf.LeafType
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.range
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

internal interface StitcherTokenFactory {
    fun create(type: LeafType, range: IntRange, text: String) : LeafToken

    fun fromAntlrToken(token: AntlrToken): LeafToken = create(LeafType(token.type), token.range, token.text)
    fun fromAntlrRule(type: LeafType, rule: ParserRuleContext): LeafToken = create(type, rule.range, rule.text)
    fun fromAntlrNode(node: TerminalNode): LeafToken = fromAntlrToken(node.symbol)

    class Inline(private val start: Int) : StitcherTokenFactory {
        override fun create(type: LeafType, range: IntRange, text: String): LeafToken = LeafToken(type, range shl start, text)
    }

    companion object : StitcherTokenFactory {
        override fun create(type: LeafType, range: IntRange, text: String): LeafToken = LeafToken(type, range, text)
    }
}