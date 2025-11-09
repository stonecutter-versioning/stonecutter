package dev.kikugie.stitcher.issue

import java.nio.file.Path

/**
 * Functional interface for handling problems reported by a [ProblemSource].
 */
public fun interface ProblemConsumer {
    public fun accept(file: Path, location: ProblemLocation, cause: ProblemCause)
}