package dev.kikugie.stitcher.util

internal fun merge(a: IntRange?, b: IntRange?): IntRange {
    val first = a ?: b ?: return IntRange.EMPTY
    val last = b ?: a ?: return IntRange.EMPTY
    return first.first..last.last
}

internal fun merge(vararg ranges: IntRange?): IntRange {
    val first = ranges.firstNotNull() ?: return IntRange.EMPTY
    val last = ranges.lastNotNull() ?: return IntRange.EMPTY
    return first.first..last.last
}