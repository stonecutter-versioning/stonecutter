package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.FileLineIndex
import java.nio.file.Path

@DslMarker @Retention(AnnotationRetention.SOURCE)
private annotation class ProblemsDsl

@ProblemsDsl
public data class ProblemTemplate(val message: String, val cause: Throwable?)

@ProblemsDsl @JvmInline
public value class ProblemLocation private constructor(private val packed: Long) {
    public constructor(line: Int, column: Int) : this((line.toLong() and 0xFFFFFFFFL) or (column.toLong() shl 32))

    public val line: Int get() = (packed and 0xFFFFFFFFL).toInt()
    public val column: Int get() = (packed ushr 32 and 0xFFFFFFFFL).toInt()

    public val isUndefined: Boolean get() = packed == -1L
}

@ProblemsDsl
internal class BailException : RuntimeException()

@ProblemsDsl
internal interface ProblemSource {
    val index: FileLineIndex

    fun at(index: Int): ProblemLocation = this.index.locate(index)
    fun at(token: StitcherToken): ProblemLocation = at(token.range.first)
    fun at(token: AntlrToken): ProblemLocation = ProblemLocation(token.line, token.charPositionInLine + 1)
    fun at(line: Int, column: Int): ProblemLocation = ProblemLocation(line, column)

    fun problem(message: String, cause: Throwable? = null): ProblemTemplate = ProblemTemplate(message, cause)
    fun accept(location: ProblemLocation, problem: ProblemTemplate)

    infix fun ProblemLocation.report(message: String): Unit = this@ProblemSource.accept(this, ProblemTemplate(message, null))
    infix fun ProblemLocation.bail(message: String): Nothing = this@ProblemSource.accept(this, ProblemTemplate(message, null)).let { throw BailException() }

    infix fun ProblemLocation.report(template: ProblemTemplate): Unit = this@ProblemSource.accept(this, template)
    infix fun ProblemLocation.bail(template: ProblemTemplate): Nothing = this@ProblemSource.accept(this, template).let { throw BailException() }

    companion object {
        inline fun problem(cause: Throwable? = null, message: () -> String): ProblemTemplate = ProblemTemplate(message(), cause)
    }
}

@ProblemsDsl
public class ProblemSink internal constructor(
    internal val file: Path,
    override val index: FileLineIndex,
    internal val consumer: ProblemConsumer
) : ProblemSource, IProblemSink {
    internal var hasFailed: Boolean = false
        private set

    override fun accept(location: ProblemLocation, problem: ProblemTemplate) {
        hasFailed = true
        consumer.accept(file, location, problem)
    }
}

@ProblemsDsl
public sealed interface IProblemSink

@ProblemsDsl
public fun interface ProblemConsumer {
    public fun accept(file: Path, location: ProblemLocation, problem: ProblemTemplate)
}