package dev.kikugie.stitcher.issue

internal data class ProblemTemplate(val message: String, val severity: ProblemSeverity = ERROR) {
    fun at(line: Int, offset: Int) = ProblemReport(message, ProblemLocation.Direct(line, offset), severity)
    fun at(index: Int) = ProblemReport(message, ProblemLocation.Lazy(index), severity)
}