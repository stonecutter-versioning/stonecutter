@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.FileLineIndex
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.absolutePathString

@DslMarker @Retention(AnnotationRetention.BINARY)
internal annotation class ProblemsDsl

@ProblemsDsl
internal class BailException : RuntimeException()

@ProblemsDsl
public data class ProblemTemplate(val message: String, val cause: Throwable?)

@ProblemsDsl
public data class ProblemLocation(val line: Int, val offset: Int, val sink: ProblemSink)

public fun interface ProblemReporter {
    public operator fun invoke(file: Path, location: ProblemLocation, template: ProblemTemplate)
}

@ProblemsDsl
public class ProblemSink internal constructor(internal val file: Path, internal val index: FileLineIndex, private val reporter: ProblemReporter) {
    public var isSuccess: Boolean = true
        private set

    internal fun report(location: ProblemLocation, template: ProblemTemplate) {
        isSuccess = false; reporter.invoke(file, location, template)
    }
}

@ProblemsDsl
internal inline fun problem(cause: Throwable? = null, message: () -> String): ProblemTemplate =
    ProblemTemplate(message(), cause)

@ProblemsDsl
internal inline fun ProblemSink.at(pos: Int): ProblemLocation =
    index.locate(pos, this)

@ProblemsDsl
internal inline fun ProblemSink.at(line: Int, column: Int): ProblemLocation =
    ProblemLocation(line, column, this)

@ProblemsDsl
internal inline fun ProblemSink.at(token: AntlrToken): ProblemLocation =
    at(token.line, token.charPositionInLine)

@ProblemsDsl
internal inline fun ProblemSink.at(token: StitcherToken): ProblemLocation =
    index.locate(token.range.first, this)

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