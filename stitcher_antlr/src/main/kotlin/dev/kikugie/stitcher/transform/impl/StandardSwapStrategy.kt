package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.util.LINE_BREAKS
import dev.kikugie.stitcher.util.WORD_BREAKS

internal object StandardSwapStrategy : SwappingStrategy {
    override fun replace(scope: String, value: String): String {
        val range = scope.run { countOffset()..<(length - reversed().countOffset()) }
        val indent = scope.lineSequence()
            .filter { it.isNotBlank() }
            .map { it.takeWhile(WORD_BREAKS::contains) }
            .minOrNull() ?: ""

        val replacement = value.replaceIndent(indent)
        return scope.replaceRange(range, replacement)
    }

    private fun CharSequence.countOffset(): Int {
        var count = 0
        var seenNewLine = false
        for (char in this) when (char) {
            in WORD_BREAKS -> if (seenNewLine) break else count++
            in LINE_BREAKS -> count++.also { seenNewLine = true }
            else -> break
        }

        return if (seenNewLine) count else 0
    }
}