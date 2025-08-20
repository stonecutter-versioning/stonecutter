@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.util

import dev.kikugie.stitcher.data.LeafToken
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.TerminalNode

internal inline val Token.length: Int get() = stopIndex - startIndex
internal inline val Token.range: IntRange get() = startIndex..stopIndex
internal inline val Token.isEOF: Boolean get() = type == Token.EOF

internal inline val ParserRuleContext.range get() = start.startIndex..stop.stopIndex
internal inline val TerminalNode.range get() = symbol.range
internal inline fun Interval.asRange(): IntRange = a..b
internal inline fun IntRange.asInterval(): Interval = Interval.of(first, last)

internal fun TerminalNode.toLeaf() =
    LeafToken(symbol.type, symbol.range, symbol.inputStream)

internal fun String.toStream(): CharStream =
    CharStreams.fromString(this)

internal fun String.toStream(source: String): CharStream =
    CharStreams.fromString(this, source)

internal operator fun CharStream.get(range: IntRange) =
    getText(range.asInterval())

internal operator fun CharStream.get(start: Int, end: Int) =
    getText(Interval.of(start, end - 1))

internal fun TokenSource.asSequence() = generateSequence { nextToken()?.takeIf { it.type != Token.EOF } }