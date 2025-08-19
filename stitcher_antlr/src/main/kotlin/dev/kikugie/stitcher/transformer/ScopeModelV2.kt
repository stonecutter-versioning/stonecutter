package dev.kikugie.stitcher.transformer

import dev.kikugie.stitcher.antlr.adapter.InlineCharStream
import org.antlr.v4.runtime.CharStream

internal class ScopeModelV2(host: CharStream, start: Int, end: Int) : AutoCloseable {
    private val inlined = InlineCharStream(host, start, end)

    override fun close() = inlined.close()
}