package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.transform.strategy.SwappingStrategy

private val INDENT_CHARACTERS: CharArray = charArrayOf(' ', '\t')

internal object StandardSwapStrategy : SwappingStrategy {
    override fun replace(scope: String, value: String): String {
        val indent = scope.lineSequence()
            .filter { it.isNotBlank() }
            .map { it.takeWhile(INDENT_CHARACTERS::contains) }
            .minOrNull() ?: ""

        return value.replaceIndent(indent)
    }
}