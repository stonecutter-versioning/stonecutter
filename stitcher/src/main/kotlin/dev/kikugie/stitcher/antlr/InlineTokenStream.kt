package dev.kikugie.stitcher.antlr

import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenFactory
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.misc.Pair

internal class InlineTokenStream(val stream: TokenStream, val offset: Int) : TokenStream by stream {
    constructor(source: TokenSource, offset: Int) : this(CommonTokenStream(source), offset)
    private val src = Pair(stream.tokenSource, stream.tokenSource.inputStream)
    private val factory: TokenFactory<*> get() = stream.tokenSource.tokenFactory
    private var cache: Array<AntlrToken?> = arrayOfNulls(4)

    private fun AntlrToken.shifted(): AntlrToken {
        val index = tokenIndex
        if (index >= cache.size) cache = cache.copyOf(cache.size * 2)
        return cache[index]
            ?: factory.create(src, type, text, channel, startIndex + offset, stopIndex + offset, line, charPositionInLine)
                .also { cache[index] = it }
    }

    override fun LT(k: Int): Token = stream.LT(k).shifted()
    override fun get(index: Int): Token = stream.get(index).shifted()
    override fun getText(interval: Interval): String = stream.getText(Interval.of(interval.a + offset, interval.b + offset))
}