package dev.kikugie.stitcher.issue

/**
 * Represents the cause of a problem.
 * @property message Issue explanation
 * @property exception Provides stacktrace if the issue is caused by an external operation
 */
public data class ProblemCause(val message: String, val exception: Throwable?)