package dev.kikugie.stitcher.issue

internal class ProblemCollector(problems: Iterable<ProblemReport> = emptyList()) {
    private val problems: MutableList<ProblemReport> = mutableListOf<ProblemReport>().apply { addAll(problems) }

    @ProblemDsl
    internal fun report(report: ProblemReport): ProblemReport = report.also {
        problems += it
    }

    @ProblemDsl
    internal fun report(report: ProblemTemplate): ProblemReport = report(report.build())

    @ProblemDsl
    internal inline fun check(condition: Boolean, provider: () -> ProblemTemplate, recovery: (ProblemReport) -> Unit) {
        if (!condition) recovery(report(provider().build()))
    }

    @ProblemDsl
    internal inline fun checkNot(condition: Boolean, provider: () -> ProblemTemplate, recovery: (ProblemReport) -> Unit = {}) {
        if (condition) recovery(report(provider().build()))
    }
}