@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.issue

@ProblemDsl
internal inline fun ProblemCollector.bail(report: ProblemTemplate): Nothing = bail(report(report))

@ProblemDsl
internal inline fun ProblemCollector.check(condition: Boolean, provider: () -> ProblemTemplate, recovery: (ProblemReport) -> Unit) {
    if (!condition) recovery(report(provider()))
}

@ProblemDsl
internal inline fun ProblemCollector.checkNot(condition: Boolean, provider: () -> ProblemTemplate, recovery: (ProblemReport) -> Unit = {}) {
    if (condition) recovery(report(provider()))
}

@ProblemDsl
internal inline fun <T : Any> ProblemCollector.checkNotNull(value: T?, provider: () -> ProblemTemplate, recovery: (ProblemReport) -> T = ::bail): T {
    return value ?: recovery(report(provider()))
}

internal class ProblemCollector(problems: Iterable<ProblemReport> = emptyList()) {
    private val problems: MutableList<ProblemReport> = mutableListOf<ProblemReport>().apply { addAll(problems) }

    @ProblemDsl
    internal fun report(report: ProblemTemplate): ProblemReport = report.build().also { problems += it }
}