package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.transform.strategy.SwappingStrategy

private val INDENT_CHARACTERS: CharArray = charArrayOf(' ', '\t')

internal object StandardSwapStrategy : SwappingStrategy {
    override fun replace(scope: String, value: String): String {
        val range = scope.run { countOffset()..<(length - reversed().countOffset()) }
        val indent = scope.lineSequence()
            .filter { it.isNotBlank() }
            .map { it.takeWhile(INDENT_CHARACTERS::contains) }
            .minOrNull() ?: ""

        val replacement = value.replaceIndent(indent)
        return scope.replaceRange(range, replacement)
    }

    private fun CharSequence.countOffset(): Int {
        var count = 0
        var seenNewLine = false
        for (char in this) when (char) {
            ' ', '\t' -> if (seenNewLine) break else count++
            '\r', '\n' -> count++.also { seenNewLine = true }
            else -> break
        }

        return if (seenNewLine) count else 0
    }
}