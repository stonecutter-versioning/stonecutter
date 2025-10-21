package dev.kikugie.stitcher.data.leaf

import dev.kikugie.stitcher.antlr.StitcherLexer

@JvmInline
internal value class LeafType(val value: Int) {
    val name: String get() = StitcherLexer.VOCABULARY.getDisplayName(value)
}