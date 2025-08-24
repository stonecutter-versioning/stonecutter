package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.data.acceptThis
import dev.kikugie.stitcher.util.get
import dev.kikugie.stitcher.util.merge
import dev.kikugie.stitcher.util.verify

private fun StringBuilder.appendToken(token: StitcherToken): StringBuilder = append(token.text)

/** Skips the empty line after the comment. */
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

internal class FileTransformer(
    val parameters: TransformParameters,
    val builder: StringBuilder = StringBuilder()
) : BlockToken.Visitor<Unit> {
    override fun visitRoot(it: BlockToken.Root) {
        for (block in it.scope) block.acceptThis()
    }

    override fun visitCode(it: BlockToken.Code) {
        builder.appendToken(it)
        it.definition.accept(ScopeTransformer(it, parameters, builder))
    }

    override fun visitContent(it: BlockToken.Content) {
        builder.appendToken(it)
    }

    override fun visitComment(it: BlockToken.Comment) {
        builder.appendToken(it)
    }
}

private class ScopeTransformer(
    val host: BlockToken.Code,
    val parameters: TransformParameters,
    val builder: StringBuilder
) : DefinitionToken.Visitor<Unit> {
    override fun visitReplacement(it: DefinitionToken.Replacement) {
        TODO("Not yet implemented")
    }

    override fun visitSwap(it: DefinitionToken.Swap) = with(builder) {
        if (it !is DefinitionToken.Swap.Opener) return

        val replacement = verify(parameters.swaps[it.identifier.text]) { "Skill issue" }
        val text = host.scope.run {
            if (isEmpty()) "" else host.source[merge(first().range, last().range)]
        }
        val start = text.countOffset()
        val end = text.length - text.reversed().countOffset()
        val fragment = parameters.replacer.replace(text.substring(start, end), replacement)
        appendRange(text, 0, start)
        append(fragment)
        appendRange(text, end, text.length)
        Unit
    }

    override fun visitCondition(it: DefinitionToken.Condition) {
        TODO("Not yet implemented")
    }

}