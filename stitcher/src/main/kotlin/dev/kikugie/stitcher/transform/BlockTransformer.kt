package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.antlr.InlineErrorListener
import dev.kikugie.stitcher.antlr.InlineTokenStream
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.SwapTemplate
import dev.kikugie.stitcher.data.composite.BlockToken
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.CommentBlock
import dev.kikugie.stitcher.data.composite.ConditionDefinition
import dev.kikugie.stitcher.data.composite.ContentBlock
import dev.kikugie.stitcher.data.composite.DefinitionToken
import dev.kikugie.stitcher.data.composite.ReplacementDefinition
import dev.kikugie.stitcher.data.composite.RootBlock
import dev.kikugie.stitcher.data.composite.SwapDefinition
import dev.kikugie.stitcher.data.eval.BlockRangeVisitor.range
import dev.kikugie.stitcher.data.eval.BlockStartVisitor.start
import dev.kikugie.stitcher.data.eval.BlockStopVisitor.stop
import dev.kikugie.stitcher.data.eval.BlockToStringVisitor.Companion.join
import dev.kikugie.stitcher.data.leaf.LeafToken
import dev.kikugie.stitcher.data.leaf.LeafType
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.issue.verifyNotNull
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.layout.LayoutParser
import dev.kikugie.stitcher.transform.impl.ExpressionEvaluator
import dev.kikugie.stitcher.util.*
import dev.kikugie.stitcher.util.range
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Pair

internal data class BlockTransformer(
    val runtime: RuntimeState,
    val params: TransformParameters,
    val factory: StitcherTokenFactory
) : BlockToken.Visitor<BlockToken> {
    private var visitedEnabledBlock: Boolean = false

    override fun visitRoot(root: RootBlock) = root.copy(scope = root.scope.map { it.accept(this) })
    override fun visitCode(code: CodeBlock) = code.copy(scope = code.definition.accept(ScopeTransformer(code)))
    override fun visitComment(comment: CommentBlock) = comment
    override fun visitContent(content: ContentBlock): BlockToken {
        if (content.leaf.text.isNotBlank()) runtime.initializeReplacements(params.replacements)
        return content
    }

    private fun List<BlockToken>.reprocess(): List<BlockToken> = buildList {
        val copy = this@BlockTransformer.copy()
        for (it in this@reprocess) this += it.accept(copy)
    }

    private inner class ScopeTransformer(val host: CodeBlock) : DefinitionToken.Visitor<List<BlockToken>> {
        override fun visitReplacement(repl: ReplacementDefinition): List<BlockToken> {
            if (runtime.replacer != null) runtime.sink.at(host.marker) report problem { "Late replacement token" }
            else runtime.includeReplacement(repl.identifier.text)
            return emptyList()
        }

        override fun visitSwap(swap: SwapDefinition): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (swap !is SwapDefinition.Opener) return emptyList()

            val identifier = swap.identifier.text
            val template = runtime.sink.verifyNotNull(params.swaps[identifier]?.processTemplate(swap.arguments)) {
                at(swap.identifier) report problem { "Unregistered swap identifier '${identifier}'" }
                return host.scope
            }

            val text = params.replacer.replace(host.scope.join(), template)
            val token = factory.create(LeafType(LayoutParser.CONTENT), host.scope.range(), text)
            return ContentBlock(token, text.isBlank()).let(::listOf)
        }

        override fun visitCondition(cond: ConditionDefinition): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (cond !is ConditionDefinition.Extension) visitedEnabledBlock = false
            if (cond is ConditionDefinition.Closer) return emptyList()

            // In an if-else chain makes the rest of the blocks disabled
            val shouldEnable = cond.expression?.accept(ExpressionEvaluator(runtime, params)) ?: true
                && !visitedEnabledBlock
            visitedEnabledBlock = shouldEnable || visitedEnabledBlock

            return when {
                shouldEnable -> if (host.scope.isCommented()) uncommentScope() else host.scope.reprocess()
                else -> if (!host.scope.isCommented()) commentScope() else host.scope
            }
        }

        private fun uncommentScope(): List<BlockToken> {
            val start = host.scope.ifEmpty { return emptyList() }.start()
            val source = UncommentingTokenSource(runtime, params, host.scope)
            return LayoutParser
                .parse(CommonTokenStream(source), runtime.sink, StitcherTokenFactory.Inline(start))
                .scope.reprocess()
        }

        private fun commentScope(): List<BlockToken> {
            val start = host.scope.ifEmpty { return emptyList() }.start()
            val blocks = host.scope.reprocess()
            val text = params.commenter.comment(blocks.join())
            val source = params.adapter.create(text.toStream(), runtime.sink)
            return LayoutParser
                .parse(CommonTokenStream(source), runtime.sink, StitcherTokenFactory.Inline(start))
                .scope
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

// TODO: Merge `ContentBlock`s at this stage
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
        is ContentBlock -> {
            yield(factory.create(source, LayoutParser.CONTENT, block.leaf.text, Token.DEFAULT_CHANNEL, block.start(), block.stop(), -1, -1))
        }
        is CommentBlock -> {
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

private fun List<BlockToken>.isCommented(): Boolean =
    all { it is CommentBlock || (it is ContentBlock && it.isBlank()) }