@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.util

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.IntStream

internal const val EOF: Char = '\u0000'

internal val LINE_BREAKS: CharArray = charArrayOf('\r', '\n')
internal val WORD_BREAKS: CharArray = charArrayOf(' ', '\t')
internal val WHITESPACES: CharArray = WORD_BREAKS + LINE_BREAKS

internal inline fun CharSequence.hasLineBreak() = any(LINE_BREAKS::contains)

internal inline fun CharStream.LC(i: Int): Char = when(val it = LA(i)) {
    IntStream.EOF -> EOF
    else -> it.toChar()
}