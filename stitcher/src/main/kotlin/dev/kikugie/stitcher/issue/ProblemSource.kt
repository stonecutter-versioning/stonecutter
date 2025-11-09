package dev.kikugie.stitcher.issue

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