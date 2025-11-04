package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.FileLineIndex
import java.nio.file.Path

/**
 * Represents the cause of a problem.
 * @property message Issue explanation
 * @property exception Provides stacktrace if the issue is caused by an external operation
 */
public data class ProblemCause(val message: String, val exception: Throwable?)

/**
 * Represents the problem location in the processed file.
 * @property line Line in the file, starting at 1
 * @property column Column in the [line], starting at 1
 * @property isUndefined Whenever the associated issue applies to the whole file
 */
@JvmInline
public value class ProblemLocation private constructor(private val packed: Long) {
    public constructor(line: Int, column: Int) : this((line.toLong() and 0xFFFFFFFFL) or (column.toLong() shl 32))

    public val line: Int get() = (packed and 0xFFFFFFFFL).toInt()
    public val column: Int get() = (packed ushr 32 and 0xFFFFFFFFL).toInt()
    public val isUndefined: Boolean get() = packed == -1L
}

/**
 * Represents an error that should be caught but not reported to avoid duplicate entries.
 */
public class BailException : RuntimeException()

/**
 * Provides an interface for reporting and managing problems within a defined source,
 * supporting categorization by specific locations and causes.
 */

public interface ProblemSource {
    public fun at(index: Int): ProblemLocation
    public fun at(line: Int, column: Int): ProblemLocation

    public fun problem(message: String, exception: Throwable? = null): ProblemCause
    public fun accept(location: ProblemLocation, cause: ProblemCause)

    public infix fun ProblemLocation.report(message: String): Unit =
        accept(this, ProblemCause(message, null))

    public infix fun ProblemLocation.bail(message: String): Nothing =
        accept(this, ProblemCause(message, null)).let { throw BailException() }

    public infix fun ProblemLocation.report(cause: ProblemCause): Unit =
        accept(this, cause)

    public infix fun ProblemLocation.bail(cause: ProblemCause): Nothing =
        accept(this, cause).let { throw BailException() }
}

/**
 * Functional interface for handling problems reported by a [ProblemSource].
 */
public fun interface ProblemConsumer {
    public fun accept(file: Path, location: ProblemLocation, cause: ProblemCause)
}

internal fun ProblemSource.at(token: StitcherToken): ProblemLocation =
    at(token.range.first)

internal fun ProblemSource.at(token: AntlrToken): ProblemLocation =
    at(token.line, token.charPositionInLine + 1)

internal inline fun <T : Any> throwRun(provider: () -> T, handler: (e: Exception) -> Nothing): T = try {
    provider()
} catch (bail: BailException) {
    throw bail
} catch (e: Exception) {
    handler(e)
}

internal inline fun <T : Any> tryRun(provider: () -> T, handler: (e: Exception) -> Unit): T? = try {
    provider()
} catch (_: BailException) {
    null
} catch (e: Exception) {
    handler(e); null
}