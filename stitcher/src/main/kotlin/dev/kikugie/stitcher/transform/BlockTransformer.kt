package dev.kikugie.stitcher.transform

import dev.kikugie.commons.takeAs
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
import dev.kikugie.stitcher.parse.inline.InlineTokenConverter
import dev.kikugie.stitcher.parse.builder.LayoutBuilder
import dev.kikugie.stitcher.transform.visitor.BlockAssembler.Companion.join
import dev.kikugie.stitcher.transform.visitor.RangeFinder.range
import dev.kikugie.stitcher.transform.impl.UncommentingTokenSource
import dev.kikugie.stitcher.transform.visitor.ExpressionEvaluator
import dev.kikugie.stitcher.util.buildString
import dev.kikugie.stitcher.util.isEOF
import dev.kikugie.stitcher.util.range
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token

private fun List<BlockToken>.join(): String = buildString {
    for (it in this@join) it.join(this)
}

private fun List<BlockToken>.isCommented(): Boolean =
    all { it is BlockToken.Comment || (it is BlockToken.Content && it.leaf.text.isBlank()) }

internal data class BlockTransformer(
    val runtime: RuntimeState,
    val params: TransformParameters,
    val converter: InlineTokenConverter
) : BlockToken.Visitor<BlockToken> {
    private var visitedEnabledBlock: Boolean = false

    override fun visitRoot(it: BlockToken.Root) = it.copy(scope = it.scope.map { it.acceptThis() })
    override fun visitCode(it: BlockToken.Code) = it.copy(scope = it.definition.accept(ScopeTransformer(it)))
    override fun visitComment(it: BlockToken.Comment) = it
    override fun visitContent(it: BlockToken.Content): BlockToken = with(it.leaf) {
        if (text.isNotBlank())
            runtime.initializeReplacements(params.replacements)

        if (runtime.replacer == null || params.replacements.isEmpty())
            return it

        val transformed = buildString(text) { runtime.replacer!!.replace(this) }
        BlockToken.Content(copy(text = transformed))
    }

    private inner class ScopeTransformer(val host: BlockToken.Code) : Visitor<List<BlockToken>> {
        override fun visitReplacement(it: Replacement): List<BlockToken> {
            if (runtime.replacer != null) runtime.sink.at(host.marker) report problem { "Late replacement token" }
            else runtime.includeReplacement(it.identifier.text)

            return emptyList()
        }

        override fun visitSwap(it: Swap): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (it !is Swap.Opener) return emptyList()

            val identifier = it.identifier.text
            val template = runtime.sink.verifyNotNull(params.swaps[identifier]?.processTemplate(it.arguments)) {
                at(it.identifier) report problem { "Unregistered swap identifier '${identifier}'" }
                return host.scope
            }

            val text = params.replacer.replace(host.scope.join(), template)
            val token = converter(LayoutBuilder.CONTENT, host.scope.range(), text)
            return listOf(BlockToken.Content(token))
        }

        override fun visitCondition(it: Condition): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (it !is Condition.Extension) visitedEnabledBlock = false
            if (it is Condition.Closer) return emptyList()

            // In an if-else chain makes the rest of the blocks disabled
            val shouldEnable = it.expression?.accept(ExpressionEvaluator(runtime, params)) ?: true
                && !visitedEnabledBlock
            visitedEnabledBlock = shouldEnable || visitedEnabledBlock
            val isCommented = host.scope.isCommented()

            return if (shouldEnable && isCommented) {
                val source = UncommentingTokenSource(runtime, params, host.scope)
                val scope = LayoutBuilder.build(CommonTokenStream(source), runtime.sink, InlineTokenConverter(host.host.closer.range.last + 1))
                scope.accept(this@BlockTransformer.copy()).takeAs<BlockToken.Root>().scope
            }
            else if (!shouldEnable && !isCommented) {
                val text = params.commenter.comment(host.scope.join())
                val source = params.adapter.create(text.toStream(), runtime.sink)
                val scope = LayoutBuilder.build(CommonTokenStream(source), runtime.sink, InlineTokenConverter(host.host.closer.range.last + 1))
                scope.accept(this@BlockTransformer.copy()).takeAs<BlockToken.Root>().scope
            }
            else host.scope
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