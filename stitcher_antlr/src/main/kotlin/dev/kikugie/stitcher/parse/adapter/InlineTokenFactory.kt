package dev.kikugie.stitcher.parse.adapter

import dev.kikugie.stitcher.util.get
import dev.kikugie.stitcher.util.range
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenFactory
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair

/**
 * Factory for creating tokens inside injected code blocks.
 *
 * Created tokens have their position properties adjusted
 * to the corresponding values in the host [stream].
 *
 * @property stream The outer input stream, which will be used to query the token text.
 * @property position The position of the inlined fragment in the outer [stream],
 * corresponding to [Token.startIndex][Token.getStartIndex].
 * @property line The line of the inlined fragment in the outer [stream],
 * corresponding to [Token.line][Token.getLine].
 * @property offset The column of the inlined fragment in the outer [stream],
 * corresponding to [Token.charPositionInLine][Token.getCharPositionInLine].
 */
internal class InlineTokenFactory(val stream: CharStream, val position: Int, val line: Int, val offset: Int) : CommonTokenFactory() {
    constructor(host: Token) : this(host.inputStream, host.startIndex, host.line, host.charPositionInLine)

    /**
     * Creates a new [CommonToken], with its position being relative to the host of the injected fragment.
     */
    override fun create(
        src: Pair<TokenSource, CharStream?>,
        type: Int,
        text: String?,
        channel: Int,
        start: Int,
        stop: Int,
        line: Int,
        charPositionInLine: Int
    ): CommonToken = CommonToken(Pair(src.a, stream), type, channel, start + position, stop + position)
        .configure(line, charPositionInLine, text)

    private fun CommonToken.configure(localLine: Int, localOffset: Int, str: String?) = apply {
        line = this@InlineTokenFactory.line + localLine - 1
        charPositionInLine = if (localLine > 1) this@InlineTokenFactory.offset + localOffset else localOffset

        if (str != null) text = str
        else if (copyText) text = stream[range]
    }
}