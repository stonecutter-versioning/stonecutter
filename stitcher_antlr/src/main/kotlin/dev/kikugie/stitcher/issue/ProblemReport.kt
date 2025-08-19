package dev.kikugie.stitcher.issue

internal data class ProblemReport(
    val message: String,
    val location: ProblemLocation,
    val severity: ProblemSeverity,
)
