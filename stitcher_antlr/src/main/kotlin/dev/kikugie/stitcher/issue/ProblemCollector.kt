package dev.kikugie.stitcher.issue

internal class ProblemCollector(problems: Iterable<ProblemReport> = emptyList()) {
    private val problems: MutableList<ProblemReport> = mutableListOf<ProblemReport>().apply { addAll(problems) }

    fun report(report: ProblemReport) = report.also {
        problems += it
    }

    @ProblemDsl
    inline fun check(condition: Boolean, provider: () -> ProblemReport, recovery: (ProblemReport) -> Unit) {
        if (!condition) recovery(report(provider()))
    }

    @ProblemDsl
    inline fun checkNot(condition: Boolean, provider: () -> ProblemReport, recovery: (ProblemReport) -> Unit = {}) {
        if (condition) recovery(report(provider()))
    }
}