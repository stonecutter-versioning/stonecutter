package dev.kikugie.stitcher.antlr.adapter

import dev.kikugie.stitcher.util.get
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenFactory
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair

class InlineTokenFactory(source: TokenSource, stream: CharStream?, val cursor: Int, val line: Int, val offset: Int) : CommonTokenFactory() {
    private val source = Pair(source, stream)

    override fun create(x1: Pair<TokenSource, CharStream?>, type: Int, text: String?, channel: Int, start: Int, stop: Int, x2: Int, offset: Int) =
        CommonToken(source, type, channel, start + cursor, stop + cursor).apply {
            line = this@InlineTokenFactory.line
            charPositionInLine = offset + this@InlineTokenFactory.offset

            if (text != null) this.text = text
            else if (copyText && source.b != null) this.text = source.b!![start + cursor, stop + cursor]
        }
}