@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import org.antlr.v4.runtime.Token
import java.nio.file.Path

@DslMarker @Retention(SOURCE)
internal annotation class ProblemsDsl

@ProblemsDsl
internal class BailException : Throwable()

@ProblemsDsl @JvmInline
internal value class ProblemLocation(val bits: Long) {
    val line: UInt inline get() = (bits ushr 32).toUInt()
    val column: UInt inline get() = bits.toUInt()

    override fun toString(): String = "ProblemLocation(line=$line, column=$column)"
}

@ProblemsDsl
internal class ProblemSink(val file: Path) {
    fun report(location: ProblemLocation, message: String, cause: Throwable? = null) {
        TODO()
    }
}

@ProblemsDsl
internal inline fun at(line: Int, column: Int): ProblemLocation =
    ProblemLocation(line.toLong() shl 32 or column.toLong())

@ProblemsDsl
internal inline fun at(token: Token): ProblemLocation =
    at(token.line, token.charPositionInLine)

@ProblemsDsl
internal inline fun at(token: StitcherToken, sink: ProblemSink): ProblemLocation =
    TODO()

@ProblemsDsl
internal inline fun ProblemSink.report(location: ProblemLocation, cause: Throwable? = null, message: () -> String): Unit =
    report(location, message(), cause)

@ProblemsDsl
internal inline fun ProblemSink.bail(location: ProblemLocation, message: String, cause: Throwable? = null): Nothing =
    report(location, message, cause).let { throw BailException() }

@ProblemsDsl
internal inline fun ProblemSink.bail(location: ProblemLocation, cause: Throwable? = null, message: () -> String): Nothing =
    bail(location, message(), cause)
