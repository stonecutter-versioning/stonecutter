package dev.kikugie.stitcher.transform

import dev.kikugie.commons.takeAs
import dev.kikugie.stitcher.antlr.InlineErrorListener
import dev.kikugie.stitcher.antlr.InlineTokenStream
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.SwapTemplate
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken.*
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.issue.verifyNotNull
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.layout.LayoutParser
import dev.kikugie.stitcher.transform.visitor.BlockAssembler.Companion.join
import dev.kikugie.stitcher.transform.visitor.ExpressionEvaluator
import dev.kikugie.stitcher.transform.visitor.RangeFinder.range
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.FileLineIndex
import dev.kikugie.stitcher.util.asSequence
import dev.kikugie.stitcher.util.buildString
import dev.kikugie.stitcher.util.errorListener
import dev.kikugie.stitcher.util.isEOF
import dev.kikugie.stitcher.util.range
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenFactory
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenFactory
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair

private fun List<BlockToken>.join(): String = buildString {
    for (it in this@join) it.join(this)
}

private fun List<BlockToken>.isCommented(): Boolean =
    all { it is BlockToken.Comment || (it is BlockToken.Content && it.leaf.text.isBlank()) }

internal data class BlockTransformer(
    val runtime: RuntimeState,
    val params: TransformParameters,
    val factory: StitcherTokenFactory
) : BlockToken.Visitor<BlockToken> {
    private var visitedEnabledBlock: Boolean = false

    override fun visitRoot(it: BlockToken.Root) = it.copy(scope = it.scope.map { it.accept(this) })
    override fun visitCode(it: BlockToken.Code) = it.copy(scope = it.definition.accept(ScopeTransformer(it)))
    override fun visitComment(it: BlockToken.Comment) = it
    override fun visitContent(it: BlockToken.Content): BlockToken = with(it.leaf) {
        if (text.isNotBlank())
            runtime.initializeReplacements(params.replacements)

        if (runtime.replacer == null || params.replacements.isEmpty())
            return it

        val transformed = buildString(text) { runtime.replacer!!.replace(this) }
        it.copy(leaf = copy(text = transformed), blank = transformed.isBlank())
    }

    private fun List<BlockToken>.reprocess(): List<BlockToken> = BlockToken.Root(this).reprocess()
    private fun BlockToken.Root.reprocess(): List<BlockToken> = accept(this@BlockTransformer.copy()).takeAs<BlockToken.Root>().scope

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
            val token = factory.create(LeafToken.Type(StitcherLexer.IDENTIFIER), host.scope.range(), text)
            return BlockToken.Content(token, text.isBlank()).let(::listOf)
        }

        override fun visitCondition(it: Condition): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (it !is Condition.Extension) visitedEnabledBlock = false
            if (it is Condition.Closer) return emptyList()

            // In an if-else chain makes the rest of the blocks disabled
            val shouldEnable = it.expression?.accept(ExpressionEvaluator(runtime, params)) ?: true
                && !visitedEnabledBlock
            visitedEnabledBlock = shouldEnable || visitedEnabledBlock

            return when {
                shouldEnable -> if (host.scope.isCommented()) uncommentScope() else host.scope.reprocess()
                else -> if (!host.scope.isCommented()) commentScope() else host.scope
            }
        }

        private fun uncommentScope(): List<BlockToken> {
            val start = host.scope.ifEmpty { return emptyList() }
                .first().range().first
            val source = UncommentingTokenSource(runtime, params, host.scope)
            return LayoutParser.parse(CommonTokenStream(source), runtime.sink, StitcherTokenFactory.Inline(start)).reprocess()
        }

        private fun commentScope(): List<BlockToken> {
            val start = host.scope.ifEmpty { return emptyList() }
                .first().range().first
            val blocks = host.scope.reprocess()
            val text = params.commenter.comment(blocks.join())
            val source = params.adapter.create(text.toStream(), runtime.sink)
            return LayoutParser.parse(CommonTokenStream(source), runtime.sink, StitcherTokenFactory.Inline(start)).scope
        }

        private fun String.processTemplate(tokens: List<LeafToken>): String {
            if (tokens.isEmpty()) return this

            // Collect insertable values
            val arguments = tokens.map {
                if (it.type.value == StitcherLexer.QUOTED) it.text.substring(1, it.range.last) else it.text
            }
            // Collect insertable tokens
            val places = SwapTemplate(this.toStream()).run {
                generateSequence { nextToken().takeUnless(Token::isEOF) }.map(Token::range).toList()
            }

            // FIXME: Not specifying arguments leaves the string the same instead of erroring
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

private class UncommentingTokenSource(val runtime: RuntimeState, val params: TransformParameters, val blocks: List<BlockToken>) : TokenSource {
    private var factory: TokenFactory<*> = CommonTokenFactory()
    private val source: Pair<TokenSource?, CharStream?> = Pair(this, runtime.input)
    private val sequence: Iterator<Token> = sequence {
        for (it in blocks) expand(it)
        yield(factory.create(Token.EOF, ""))
    }.iterator()

    private lateinit var token: Token

    override fun nextToken(): Token = sequence.next().also { token = it }
    override fun getLine(): Int = if (::token.isInitialized) token.line else 1
    override fun getCharPositionInLine(): Int = if (::token.isInitialized) token.charPositionInLine else 0
    override fun getInputStream(): CharStream = runtime.input
    override fun getSourceName(): String = runtime.input.sourceName
    override fun getTokenFactory(): TokenFactory<*> = factory
    override fun setTokenFactory(factory: TokenFactory<*>) { this.factory = factory }

    private suspend fun SequenceScope<AntlrToken>.expand(block: BlockToken): Unit = when (block) {
        is BlockToken.Content -> {
            val range = block.range()
            yield(factory.create(source, LayoutParser.CONTENT, block.leaf.text, Token.DEFAULT_CHANNEL, range.first, range.last, -1, -1))
        }
        is BlockToken.Comment -> {
            val start = block.body.range.first
            val content = params.uncommenter.uncomment(block.body.text, block.opener.text, block.closer.text).toStream()
            val lexer = params.adapter.create(content, runtime.sink).apply {
                scanner.errorListener(InlineErrorListener(runtime.sink, FileLineIndex(content), start))
            }
            yieldAll(InlineTokenStream(lexer, start).asSequence())
        }
        else -> error("Unexpected block type: ${block::class.simpleName}")
    }
}