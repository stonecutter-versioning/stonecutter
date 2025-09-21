package dev.kikugie.stitcher.parse.adapter

import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenFactory
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.misc.Pair

private fun AntlrToken.copyWith(factory: TokenFactory<*>, offset: Int): AntlrToken =
    factory.create(Pair(tokenSource, inputStream), type, text, channel, startIndex + offset, stopIndex + offset, line, charPositionInLine)

internal class InlineTokenStream(val stream: TokenStream, val factory: TokenFactory<*>, val offset: Int) : TokenStream by stream {
    private val cache: MutableMap<Int, AntlrToken> = mutableMapOf()

    override fun LT(k: Int): Token = stream.LT(k).shifted()
    override fun get(index: Int): Token = stream.get(index).shifted()
    override fun getText(interval: Interval): String = stream.getText(Interval.of(interval.a + offset, interval.b + offset))

    private fun AntlrToken.shifted() = cache.computeIfAbsent(tokenIndex) { copyWith(factory, offset) }
}