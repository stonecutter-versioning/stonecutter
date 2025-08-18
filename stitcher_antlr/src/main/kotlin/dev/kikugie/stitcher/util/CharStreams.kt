@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.util

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.IntStream

internal const val EOF: Char = '\u0000'

internal inline fun CharStream.LC(i: Int): Char = when(val it = LA(i)) {
    IntStream.EOF -> EOF
    else -> it.toChar()
}

internal inline fun CharStream.skipWhile(condition: (Char) -> Boolean): Boolean {
    while (true) {
        val char = LC(1)
        if (char == EOF) return false
        if (!condition(char)) break
        consume()
    }
    return true
}

internal fun CharStream.skipLeadingSpaces(vararg chars: Char): Boolean {
    while (true) when(LC(1)) {
        EOF -> return false
        in chars -> consume()
        else -> break
    }
    return true
}