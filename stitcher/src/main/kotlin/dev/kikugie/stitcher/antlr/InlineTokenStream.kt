package dev.kikugie.stitcher.antlr

import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.misc.Pair

internal class InlineTokenStream(val source: Pair<TokenSource, CharStream>, val stream: TokenStream, val offset: Int, val location: ProblemLocation) : TokenStream by stream {
    constructor(source: TokenSource, input: CharStream, offset: Int, location: ProblemLocation) : this(Pair(source, input), CommonTokenStream(source), offset, location)
    private val factory: TokenFactory<*> get() = stream.tokenSource.tokenFactory
    private var cache: Array<AntlrToken?> = arrayOfNulls(4)

    override fun LT(k: Int): Token = getCached(index().coerceAtLeast(0) + k - 1, stream.LT(k))
    override fun get(index: Int): Token = getCached(index, stream.get(index))
    override fun getText(interval: Interval): String = stream.getText(Interval.of(interval.a + offset, interval.b + offset))

    private fun getCached(index: Int, token: AntlrToken): AntlrToken {
        if (index >= cache.size) cache = cache.copyOf(cache.size * 2)
        return cache[index] ?: shiftToken(token).also { cache[index] = it }
    }

    private fun shiftToken(token: AntlrToken): AntlrToken {
        val startIndex = token.startIndex + offset
        val stopIndex = token.stopIndex + offset

        val line = if (location.isUndefined) -1 else token.line + location.line - 1
        val column = when {
            location.isUndefined -> -1
            token.line > 1 -> token.charPositionInLine
            else -> token.charPositionInLine + location.column - 1
        }

        return factory.create(source, token.type, token.text, token.channel, startIndex, stopIndex, line, column)
    }
}