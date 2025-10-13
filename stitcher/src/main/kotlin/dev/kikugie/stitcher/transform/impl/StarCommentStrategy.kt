package dev.kikugie.stitcher.transform.impl

import dev.kikugie.commons.text.countMatching
import dev.kikugie.commons.text.getOrDefault
import dev.kikugie.commons.then
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import dev.kikugie.stitcher.util.LINE_BREAKS
import dev.kikugie.stitcher.util.WORD_BREAKS
import dev.kikugie.stitcher.util.buildString

private const val SUPERSCRIPT_OOB = "Unable to nest at level %d - max is 9. Maybe consider refactoring your code?"
private val SUPERSCRIPT_NUMBERS: CharArray = charArrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')

private fun StringBuilder.matchesAt(index: Int, char: Char): Boolean =
    if (index in indices) char == get(index) else false


internal fun StringBuilder.readSuperScript(index: Int): Int =
    SUPERSCRIPT_NUMBERS.indexOf(getOrDefault(index)).coerceAtLeast(0)

// Returns true if the length was modified
internal fun StringBuilder.removeSuperScript(index: Int): Boolean = when {
    index < 0 || index >= length -> false
    getOrDefault(index) !in SUPERSCRIPT_NUMBERS -> false
    else -> true.also { deleteAt(index) }
}

internal fun StringBuilder.writeSuperScriptBefore(index: Int, depth: Int): Boolean {
    require(depth in 0..9) { SUPERSCRIPT_OOB.format(depth) }
    val char = SUPERSCRIPT_NUMBERS[depth]
    return when {
        index == 0 -> true.also { insert(0, char)}
        this[index - 1] in SUPERSCRIPT_NUMBERS -> false.also { this[index - 1] = char }
        else -> true.also { insert(index, char)}
    }
}
internal fun StringBuilder.writeSuperScriptAfter(index: Int, depth: Int): Boolean {
    require(depth in 0..9) { SUPERSCRIPT_OOB.format(depth) }
    val char = SUPERSCRIPT_NUMBERS[depth]
    return when {
        index == lastIndex -> true.also { append(char) }
        this[index + 1] in SUPERSCRIPT_NUMBERS -> false.also { this[index + 1] = char }
        else -> true.also { insert(index + 1, char)}
    }
}

private fun StringBuilder.applyCommentDepth(from: Char, to: Char, surrounder: Char) {
    val chars = charArrayOf(from, to)
    var index = 0
    while (true) {
        index = indexOfAny(chars, index)
        if (index < 0) break
        else if (matchesAt(index - 1, surrounder)) when (this[index]) {
            from -> this[index] = to
            to -> if (writeSuperScriptAfter(index, readSuperScript(index + 1) + 1))
                index++
        }
        else if (matchesAt(index + 1, surrounder)) when (this[index]) {
            from -> this[index] = to
            to -> if (writeSuperScriptBefore(index, readSuperScript(index - 1) + 1))
                index++
        }
        index++
    }

}

private fun StringBuilder.removeCommentDepth(from: Char, to: Char, surrounder: Char) {
    var index = 0
    while (true) {
        index = indexOf(from, index)
        if (index < 0) break
        else if (matchesAt(index - 1, surrounder)) when (val depth = readSuperScript(index + 1)) {
            0, 1 -> {
                if (depth == 0) this[index] = to
                removeSuperScript(index + 1)
            }

            else -> writeSuperScriptAfter(index, depth - 1)
        }
        else if (matchesAt(index + 1, surrounder)) when (val depth = readSuperScript(index - 1)) {
            0, 1 -> {
                if (depth == 0) this[index] = to
                if (removeSuperScript(index - 1)) index++
            }

            else -> writeSuperScriptBefore(index, depth - 1)
        }
        index++
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