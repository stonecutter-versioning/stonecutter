package dev.kikugie.stitcher.transformer

import dev.kikugie.commons.ranges.extend

interface SourceTransformation {
    val sourceRange: IntRange
    val resultRange: IntRange
}

data class InsertTransformation(val index: Int, val value: String) : SourceTransformation {
    override val sourceRange: IntRange get() = index extend 0
    override val resultRange: IntRange get() = index extend value.length
}

data class RemoveTransformation(val range: IntRange): SourceTransformation {
    override val sourceRange: IntRange get() = range
    override val resultRange: IntRange get() = range.first extend 0
}

data class ReplaceTransformation(val range: IntRange, val value: String) : SourceTransformation {
    override val sourceRange: IntRange get() = range
    override val resultRange: IntRange get() = range.first extend value.length
}