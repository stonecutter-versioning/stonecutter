package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.SwapTemplate
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken.*
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.acceptThis
import dev.kikugie.stitcher.util.get
import dev.kikugie.stitcher.util.isEOF
import dev.kikugie.stitcher.util.merge
import dev.kikugie.stitcher.util.range
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.Token

internal class FileTransformer(val parameters: TransformParameters) : BlockToken.Visitor<String> {
    override fun visitRoot(it: BlockToken.Root): String = buildString { for (block in it.scope) append(block.acceptThis()) }
    override fun visitContent(it: BlockToken.Content): String = it.text
    override fun visitComment(it: BlockToken.Comment): String = it.text
    override fun visitCode(it: BlockToken.Code) = it.text + it.definition.accept(DefinitionTransformer(it))

    private inner class DefinitionTransformer(val host: BlockToken.Code) : Visitor<String> {
        override fun visitSwap(it: Swap): String {
            if (it !is Swap.Opener) return ""

            val template = parameters.swaps[it.identifier.text]!!
                .run { processTemplate(this, it.arguments) }
            val text = host.scope
                .run { if (isEmpty()) "" else host.source[merge(first(), last())] }
            return parameters.replacer.replace(text, template)
        }

        override fun visitReplacement(it: Replacement): String {
            TODO("Not yet implemented")
        }

        override fun visitCondition(it: Condition): String {
            TODO("Not yet implemented")
        }

        private fun processTemplate(template: String, tokens: List<LeafToken>): String {
            if (tokens.isEmpty()) return template

            val arguments = tokens.map { if (it.type == StitcherParser.QUOTED) it.text.substring(1, it.range.last) else it.text }
            val places = SwapTemplate(template.toStream()).run {
                generateSequence { nextToken().takeUnless(Token::isEOF) }.map(Token::range).toList()
            }

            val builder = StringBuilder(template)
            for (range in places.asReversed()) {
                // FIXME: Use checked conversion and list getter
                val index = template.substring(range).substring(1).toInt()
                val value = arguments[index - 1]
                builder.replace(range.first, range.last + 1 , value)
            }
            return builder.toString()
        }
    }
}