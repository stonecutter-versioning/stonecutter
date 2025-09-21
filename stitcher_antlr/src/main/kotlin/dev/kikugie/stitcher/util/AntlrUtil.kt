@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.util

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.misc.Pair
import org.antlr.v4.runtime.tree.TerminalNode

internal typealias AntlrToken = Token

internal inline val Token.length: Int get() = stopIndex - startIndex
internal inline val Token.range: IntRange get() = startIndex..stopIndex
internal inline val Token.isEOF: Boolean get() = type == Token.EOF

internal inline val ParserRuleContext.range get() = start.startIndex..stop.stopIndex
internal inline val TerminalNode.range get() = symbol.range
internal inline fun Interval.asRange(): IntRange = a..b
internal inline fun IntRange.asInterval(): Interval = Interval.of(first, last)

internal fun String.toStream(): CharStream =
    CharStreams.fromString(this)

internal fun String.toStream(source: String): CharStream =
    CharStreams.fromString(this, source)

internal inline operator fun CharStream.get(range: IntRange) =
    getText(range.asInterval())

internal inline operator fun CharStream.get(start: Int, end: Int) =
    getText(Interval.of(start, end - 1))

internal fun TokenSource.asSequence() = generateSequence { nextToken()?.takeIf { it.type != Token.EOF } }

internal fun <T : AntlrToken> TokenFactory<T>.create(src: TokenSource, type: Int, start: Int, stop: Int, line: Int, offset: Int): T =
    create(Pair(src, null), type, null, Token.DEFAULT_CHANNEL, start, stop, line, offset)