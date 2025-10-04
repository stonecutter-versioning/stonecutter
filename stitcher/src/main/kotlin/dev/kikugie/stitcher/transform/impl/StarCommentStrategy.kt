package dev.kikugie.stitcher.transform.impl

import dev.kikugie.commons.text.countMatching
import dev.kikugie.commons.text.getOrDefault
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import dev.kikugie.stitcher.util.LINE_BREAKS
import dev.kikugie.stitcher.util.WORD_BREAKS
import dev.kikugie.stitcher.util.buildString

private val SUPERSCRIPT_NUMBERS: CharArray = charArrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')

private fun StringBuilder.matchesAt(index: Int, char: Char): Boolean =
    if (index in indices) char == get(index) else false

private fun StringBuilder.readSuperScript(index: Int): Int =
    SUPERSCRIPT_NUMBERS.indexOf(getOrDefault(index)).coerceAtLeast(0)

private fun StringBuilder.removeSuperScript(index: Int): Boolean =
    (index in indices && getOrDefault(index) !in SUPERSCRIPT_NUMBERS).also { if (it) deleteAt(index) }

private fun StringBuilder.writeSuperScriptBefore(index: Int, depth: Int): Boolean {
    require(depth in 0..9)
    val char = SUPERSCRIPT_NUMBERS[depth]
    return when {
        index > 0 && get(index - 1) in SUPERSCRIPT_NUMBERS -> {
            set(index - 1, char); false
        }

        else -> {
            insert(0, char); true
        }
    }
}

private fun StringBuilder.writeSuperScriptAfter(index: Int, depth: Int): Boolean {
    require(depth in 0..9)
    val char = SUPERSCRIPT_NUMBERS[depth]
    return when {
        index >= length -> {
            append(char); true
        }

        get(index + 1) in SUPERSCRIPT_NUMBERS -> {
            set(index + 1, char); false
        }

        else -> {
            insert(index + 1, char); true
        }
    }
}

private fun StringBuilder.applyCommentDepth(from: Char, to: Char, surrounder: Char) {
    val lookup = charArrayOf(from, to)
    var index = 0
    while (indexOfAny(lookup, index).also { index = it } >= 0) when {
        matchesAt(index - 1, surrounder) -> {
            if (get(index) == from) set(index, to)
            else if (writeSuperScriptAfter(index, readSuperScript(index + 1) + 1)) index++
            index++
        }

        matchesAt(index + 1, surrounder) -> {
            if (get(index) == from) set(index, to)
            else if (writeSuperScriptAfter(index, readSuperScript(index - 1) + 1)) index++
            index++
        }

        else -> index++
    }
}

private fun StringBuilder.removeCommentDepth(from: Char, to: Char, surrounder: Char) {
    var index = 0
    while (indexOf(from).also { index = it } >= 0) when {
        matchesAt(index - 1, surrounder) -> with(readSuperScript(index + 1)) {
            if (this == 0) set(index, to)
            if (this in 0..1) removeSuperScript(index + 1)
            else writeSuperScriptAfter(index, this - 1)
            index++
        }

        matchesAt(index + 1, surrounder) -> with(readSuperScript(index + 1)) {
            if (this == 0) set(index, to)
            if (this in 0..1) removeSuperScript(index - 1)
            else writeSuperScriptBefore(index, this - 1)
            index++
        }

        else -> index++
    }
}

// TODO: Make it more generic
public class StarCommentStrategy(private val flattenComments: Boolean) : CommentingStrategy, UncommentingStrategy {
    override fun comment(scope: String): String = buildString(scope) {
        if (flattenComments) applyCommentDepth(from = '*', to = '^', surrounder = '/')
        insert(countMatching(*WORD_BREAKS), "/*").append("*/")
    }

    override fun uncomment(scope: String, opener: String, closer: String): String = buildString(scope) {
        removeCommentDepth(from = '^', to = '*', surrounder = '/')
        if (LINE_BREAKS.any(closer::contains)) append(closer)
    }
}