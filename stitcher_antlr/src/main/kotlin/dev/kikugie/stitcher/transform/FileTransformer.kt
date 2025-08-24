package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.DefinitionToken.*
import dev.kikugie.stitcher.data.acceptThis
import dev.kikugie.stitcher.util.get
import dev.kikugie.stitcher.util.merge

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

internal class FileTransformer(val parameters: TransformParameters) : BlockToken.Visitor<String> {
    override fun visitRoot(it: BlockToken.Root): String = it.scope.joinToString("") { it.acceptThis() }
    override fun visitContent(it: BlockToken.Content): String = it.text
    override fun visitComment(it: BlockToken.Comment): String = it.text
    override fun visitCode(it: BlockToken.Code) = it.text + it.definition.accept(DefinitionTransformer(it))

    private inner class DefinitionTransformer(val host: BlockToken.Code) : DefinitionToken.Visitor<String> {
        override fun visitSwap(it: Swap): String {
            if (it !is Swap.Opener) return ""

            val template = parameters.swaps[it.identifier.text]!!
            val text = host.scope.run { if (isEmpty()) "" else host.source[merge(first().range, last().range)] }
            val range = text.run { countOffset()..<(length - reversed().countOffset()) }
            val replacement = parameters.replacer.replace(text.substring(range), template)
            return text.replaceRange(range, replacement)
        }

        override fun visitReplacement(it: Replacement): String {
            TODO("Not yet implemented")
        }

        override fun visitCondition(it: Condition): String {
            TODO("Not yet implemented")
        }
    }
}