package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.SwapTemplate
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken.*
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.acceptThis
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.issue.verifyNotNull
import dev.kikugie.stitcher.transform.impl.ExpressionEvaluator
import dev.kikugie.stitcher.util.isEOF
import dev.kikugie.stitcher.util.merge
import dev.kikugie.stitcher.util.range
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.Token

private fun List<BlockToken>.join(): String = joinToString(transform = BlockToken::text)

private fun List<BlockToken>.isCommented(): Boolean = all { it is BlockToken.Comment || (it is BlockToken.Content && it.leaf.text.isBlank()) }

internal class BlockTransformer(val parameters: TransformParameters) : BlockToken.Visitor<BlockToken> {
    private var visitedEnabledBlock: Boolean = false

    override fun visitRoot(it: BlockToken.Root) = it.copy(scope = it.scope.map { it.acceptThis() })
    override fun visitCode(it: BlockToken.Code) = it.copy(scope = it.definition.accept(ScopeTransformer(it)))
    override fun visitContent(it: BlockToken.Content) = it
    override fun visitComment(it: BlockToken.Comment) = it

    private inner class ScopeTransformer(val host: BlockToken.Code) : Visitor<List<BlockToken>> {
        override fun visitReplacement(it: Replacement): List<BlockToken> = emptyList()
        override fun visitSwap(it: Swap): List<BlockToken> {
            if (it !is Swap.Opener) return emptyList()

            val identifier = it.identifier.text
            val template = parameters.sink.verifyNotNull(parameters.swaps[identifier]?.processTemplate(it.arguments)) {
                at(it.identifier) report problem { "Unregistered swap identifier '${identifier}'" }
                return host.scope
            }

            val text = parameters.replacer.replace(host.scope.join(), template)
            // FIXME: Handle injected tokens having different source and range
            return listOf(BlockToken.Content(text.indices, text.toStream()))
        }

        override fun visitCondition(it: Condition): List<BlockToken> {
            if (it !is Condition.Extension) visitedEnabledBlock = false
            if (it is Condition.Closer) return emptyList()

            // In an if-else chain makes the rest of the blocks disabled
            val requestedState = it.expression!!.accept(ExpressionEvaluator(parameters))
                && !visitedEnabledBlock
            visitedEnabledBlock = requestedState || visitedEnabledBlock
        }

        private fun String.processTemplate(tokens: List<LeafToken>): String {
            if (tokens.isEmpty()) return this

            // Collect insertable values
            val arguments = tokens.map {
                if (it.type == StitcherParser.QUOTED) it.text.substring(1, it.range.last) else it.text
            }
            // Collect insertable tokens
            val places = SwapTemplate(this.toStream()).run {
                generateSequence { nextToken().takeUnless(Token::isEOF) }.map(Token::range).toList()
            }

            val builder = StringBuilder(this)
            for (range in places.asReversed()) {
                // FIXME: Use checked conversion and list getter
                val index = substring(range).substring(1).toInt()
                val value = arguments[index - 1]
                builder.replace(range.first, range.last + 1, value)
            }
            return builder.toString()
        }
    }
}