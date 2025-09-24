@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.Token
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DslMarker @Retention(AnnotationRetention.BINARY)
internal annotation class ProblemsDsl

@ProblemsDsl
internal class BailException : RuntimeException()

@ProblemsDsl
internal data class ProblemTemplate(val message: String, val cause: Throwable?)

@ProblemsDsl
internal data class ProblemLocation(val line: UInt, val column: UInt, val sink: ProblemSink)

@ProblemsDsl
internal class ProblemSink(val file: Path) {
    fun report(location: ProblemLocation, template: ProblemTemplate) {
        System.err.println("At $location: $template")
    }
}

@ProblemsDsl
internal inline fun problem(cause: Throwable? = null, message: () -> String): ProblemTemplate =
    ProblemTemplate(message(), cause)

@ProblemsDsl
internal inline fun ProblemSink.at(line: Int, column: Int): ProblemLocation =
    ProblemLocation(line.toUInt(), column.toUInt(), this)

@ProblemsDsl
internal inline fun ProblemSink.at(token: AntlrToken): ProblemLocation =
    at(token.line, token.charPositionInLine)

@ProblemsDsl
internal inline fun ProblemSink.at(token: StitcherToken): ProblemLocation =
    ProblemLocation(1u, 0u, this)

@ProblemsDsl
internal inline infix fun ProblemLocation.report(template: ProblemTemplate): Unit =
    sink.report(this, template)

@ProblemsDsl
internal inline infix fun ProblemLocation.bail(template: ProblemTemplate): Nothing =
    report(template).let { throw BailException() }

@OptIn(ExperimentalContracts::class)
@ProblemsDsl
internal inline fun <T : Any> ProblemSink.verifyNotNull(value: T?, builder: ProblemSink.() -> Nothing): T {
    contract {
        returns() implies (value != null)
        callsInPlace(builder, InvocationKind.AT_MOST_ONCE)
    }
    return value ?: builder()
}