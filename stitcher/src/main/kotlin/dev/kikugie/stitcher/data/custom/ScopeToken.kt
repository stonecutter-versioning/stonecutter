package dev.kikugie.stitcher.data.custom

import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.data.leaf.LeafToken
import dev.kikugie.stitcher.util.merge

private fun LeafToken.unbox() = when(type.value) {
    StitcherLexer.IDENTIFIER -> text
    StitcherLexer.QUOTED -> text.run { substring(1, lastIndex) }
    else -> error("Invalid type ${type.name}")
}

internal sealed interface ScopeToken : StitcherToken

internal data class ClosedScope(val marker: LeafToken) : ScopeToken, StitcherToken by marker

internal data class WordScope(val marker: LeafToken, val plus: LeafToken? = null, val literal: LeafToken? = null) : ScopeToken {
    override val range: IntRange get() = merge(marker.range, plus?.range, literal?.range)
    override val text: String get() = buildString {
        append(marker.text)
        if (plus != null) append(plus.text)
        if (literal != null) append(" " + literal.text)
    }

    val isCapturing: Boolean get() = plus != null
    val expectedStr: String get() = literal?.unbox().orEmpty()
}