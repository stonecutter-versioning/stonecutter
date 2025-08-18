package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.util.get
import org.antlr.v4.runtime.CharStream

internal interface StitcherToken {
    val range: IntRange

    val source: CharStream

    val text: String get() = source[range]
}