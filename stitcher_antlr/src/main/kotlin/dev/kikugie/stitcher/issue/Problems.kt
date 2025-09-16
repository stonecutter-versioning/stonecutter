@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import org.antlr.v4.runtime.Token
import java.nio.file.Path
import kotlin.contracts.contract

@DslMarker @Retention(SOURCE)
internal annotation class ProblemsDsl

@ProblemsDsl
internal class BailException : Throwable()

@ProblemsDsl
internal data class ProblemTemplate(val message: String, val cause: Throwable?)

@ProblemsDsl
internal data class ProblemLocation(val line: UInt, val column: UInt, val sink: ProblemSink)

@ProblemsDsl
internal class ProblemSink(val file: Path) {
    fun report(location: ProblemLocation, template: ProblemTemplate) {
        TODO()
    }
}

@ProblemsDsl
internal inline fun problem(cause: Throwable? = null, message: () -> String): ProblemTemplate =
    ProblemTemplate(message(), cause)

@ProblemsDsl
internal inline fun ProblemSink.at(line: Int, column: Int): ProblemLocation =
    ProblemLocation(line.toUInt(), column.toUInt(), this)

@ProblemsDsl
internal inline fun ProblemSink.at(token: Token): ProblemLocation =
    at(token.line, token.charPositionInLine)

@ProblemsDsl
internal inline fun ProblemSink.at(stitcherToken: StitcherToken): ProblemLocation =
    TODO()

@ProblemsDsl
internal inline infix fun ProblemLocation.report(template: ProblemTemplate): Unit =
    sink.report(this, template)

@ProblemsDsl
internal inline infix fun ProblemLocation.bail(template: ProblemTemplate): Nothing =
    report(template).let { throw BailException() }
