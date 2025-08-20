@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.issue

@ProblemDsl
internal inline fun bail(report: ProblemReport? = null): Nothing {
    throw ReportedBailException(report)
}

@ProblemDsl
internal inline fun unsupported(reason: String): Nothing {
    throw UnsupportedOperationException(reason)
}

internal class ReportedBailException(val report: ProblemReport? = null) : Exception(report?.message)