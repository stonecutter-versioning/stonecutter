package dev.kikugie.stitcher.antlr.adapter

import dev.kikugie.stitcher.data.StitcherToken
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.Interval

/**
 * A [CharStream] implementation that represents a substream of a larger [CharStream][host].
 *
 * Delegates most functionality directly to the [host], while restricting access to the indices
 * between [start] and [end].
 *
 * @property host The original stream used as the source for this substream.
 * @property start The starting index of the substream in the host stream.
 * @property end The ending index (exclusive) of the substream in the host stream.
 */
internal class InlineCharStream(val host: CharStream, val start: Int, val end: Int) : AutoCloseable, CharStream by host {
    constructor(host: Token) : this(host.inputStream, host.startIndex, host.stopIndex + 1)
    constructor(host: StitcherToken) : this(host.source, host.range.first, host.range.last + 1)
    private val marker: Int = host.mark()
    private val index: Int = host.index()

    init {
        host.seek(start)
    }

    inline operator fun <T> invoke(action: InlineCharStream.() -> T): T = use(action)

    override fun size(): Int = end - start + 1 // With the EOF
    override fun index(): Int = host.index() - start

    override fun getText(interval: Interval): String =
        host.getText(Interval.of(interval.a + start, interval.b + start))

    override fun consume() {
        check(host.index() < end) { "cannot consume EOF" }
        host.consume()
    }

    override fun LA(i: Int): Int = when (host.index()) {
        end -> IntStream.EOF
        else -> host.LA(i)
    }

    override fun seek(index: Int) {
        require(index >= 0) { "Index must not be negative" }
        host.seek(index.coerceAtMost(end - start) + start)
    }

    override fun close() {
        host.seek(index)
        host.release(marker)
    }
}