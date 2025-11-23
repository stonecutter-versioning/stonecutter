package dev.kikugie.stitcher.transform

import dev.kikugie.commons.applyIf
import dev.kikugie.stitcher.antlr.*
import dev.kikugie.stitcher.data.composite.*
import dev.kikugie.stitcher.data.eval.BlockIsBlankVisitor.isBlank
import dev.kikugie.stitcher.data.eval.BlockIsBlankVisitor.isNotBlank
import dev.kikugie.stitcher.data.eval.BlockRangeVisitor.range
import dev.kikugie.stitcher.data.eval.BlockStartVisitor.start
import dev.kikugie.stitcher.data.eval.BlockToStringVisitor.Companion.join
import dev.kikugie.stitcher.data.leaf.LeafToken
import dev.kikugie.stitcher.data.leaf.LeafType
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.adapter.ScannerAdapter
import dev.kikugie.stitcher.parser.layout.LayoutParser
import dev.kikugie.stitcher.transform.impl.ExpressionEvaluator
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ListTokenSource
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.misc.Pair
import java.util.*

internal data class BlockTransformer(
    val runtime: RuntimeState,
    val params: TransformParameters,
    val factory: StitcherTokenFactory
) : BlockToken.Visitor<BlockToken>, ProblemSource by runtime.problems {
    private val evaluator: ExpressionEvaluator by lazy { ExpressionEvaluator(params, runtime) }
    private var visitedEnabledBlock: Boolean = false
    private var shouldApplyReplacements: Boolean = true

    override fun visitRoot(root: RootBlock) = root.copy(scope = root.scope.map { it.accept(this) })
    override fun visitCode(code: CodeBlock) = code.copy(scope = code.definition.accept(ScopeTransformer(code)))
    override fun visitComment(comment: CommentBlock): BlockToken {
        if (comment.opener != null && comment.closer != null) return comment

        val start = comment.start()
        val closer = comment.closer?.text.orEmpty()
        var content = params.commenter.comment(comment.body.text, false)
        if (closer.hasLineBreak()) content += closer

        // TODO: Check if reparsing the comment is needed
        return factory.create(LeafType(LayoutParser.CONTENT), comment.range(), content).let(::ContentBlock)
    }

    override fun visitContent(content: ContentBlock): BlockToken {
        if (content.isBlank())
            return content

        runtime.initializeReplacements(params.replacements)
        if (!shouldApplyReplacements || !params.replacements.isNotEmpty())
            return content

        val modified = buildString(content.join()) { runtime.replacer!!.replace(this) }
        return ContentBlock(content.leaf.copy(text = modified)).inherit(content)
    }

    private fun visitScope(tokens: Iterable<BlockToken>, link: Boolean): List<BlockToken> {
        val copy = this@BlockTransformer.copy().apply { shouldApplyReplacements = link }
        return tokens.map { it.accept(copy) }.applyIf(link, BlockToken::link)
    }

    private inner class ScopeTransformer(val host: CodeBlock) : DefinitionToken.Visitor<List<BlockToken>> {
        override fun visitReplacement(repl: ReplacementDefinition): List<BlockToken> {
            if (runtime.replacer != null) at(host.marker) report "Late replacement token"
            else runtime.includeReplacement(repl.identifier.text)
            return emptyList()
        }

        override fun visitSwap(swap: SwapDefinition): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (swap !is SwapDefinition.Opener) return emptyList()

            val template = params.swaps[swap.identifier.text]
            if (template == null) {
                at(swap.identifier) report "Unresolved swap identifier '${swap.identifier.text}'"
                return host.scope
            }

            val replacement = processTemplate(template, swap.arguments)
            val content = params.replacer.replace(host.scope.join(), replacement)
            val token = factory.create(LeafType(LayoutParser.CONTENT), host.scope.range(), content)
            return ContentBlock(token).let(::listOf)
        }

        override fun visitCondition(cond: ConditionDefinition): List<BlockToken> {
            runtime.initializeReplacements(params.replacements)
            if (cond !is ConditionDefinition.Extension) visitedEnabledBlock = false
            if (cond is ConditionDefinition.Closer) return emptyList()

            // In an if-else chain makes the rest of the blocks disabled
            val shouldEnable = cond.expression?.accept(evaluator) ?: true
                && !visitedEnabledBlock
            visitedEnabledBlock = shouldEnable || visitedEnabledBlock

            return when {
                shouldEnable -> if (host.scope.isCommented()) uncommentScope() else visitScope(host.scope, true)
                else -> if (!host.scope.isCommented()) commentScope() else host.scope
            }
        }

        private fun uncommentScope(): List<BlockToken> {
            if (host.scope.isEmpty()) return emptyList()

            val stream: TokenStream = BlockUncommenter(host.scope, params, runtime).let(::CommonTokenStream)
            val factory: StitcherTokenFactory = StitcherTokenFactory.Inline(host.start())
            val tokens: List<BlockToken> = LayoutParser.parse(stream, runtime.input, runtime.problems, factory).scope
            return if (tokens.isEmpty()) emptyList() else visitScope(tokens, true)
        }

        private fun commentScope(): List<BlockToken> {
            if (host.scope.isEmpty()) return emptyList()

            val start: Int = host.start()
            val reprocessed: List<BlockToken> = visitScope(host.scope, false)
            val content: String = params.commenter.comment(reprocessed.join(), host.definition.opener == null)
            return factory.create(LeafType(LayoutParser.CONTENT), host.range(), content).let { listOf(ContentBlock(it)) }

            // TODO: Check if reparsing the comment is needed
            // val content: CharStream = params.commenter.comment(reprocessed.join(), false).toStream(runtime.input.sourceName)
            // val stream: TokenStream = params.adapter.create(content, runtime).let { InlineTokenStream(it, runtime.input, start, at(start)) }
            // return LayoutParser.parse(stream, runtime.input, runtime, factory).scope
        }
    }
}

private class BlockUncommenter(
    blocks: List<BlockToken>,
    val parameters: TransformParameters,
    val runtime: RuntimeState,
    list: MutableList<AntlrToken> = mutableListOf()
) : ListTokenSource(list) {
    init {
        val source: Pair<TokenSource, CharStream> = Pair(this, runtime.input)
        for (it: BlockToken in blocks) match(list, source, it)
    }

    private fun match(tokens: MutableList<AntlrToken>, source: Pair<TokenSource, CharStream>, block: BlockToken): Unit = when (block) {
        is ContentBlock -> {
            tokens += tokenFactory.create(source, LayoutParser.CONTENT, block.leaf.text, block.leaf.range, runtime.at(block.leaf))
        }

        is CommentBlock -> {
            val start: Int = block.body.range.first
            val content: CharStream = parameters.uncommenter.uncomment(block.body.text, block.opener?.text.orEmpty(), block.closer?.text.orEmpty())
                .toStream(runtime.input.sourceName)
            val scanner: ScannerAdapter = parameters.adapter.create(content, runtime).apply {
                scanner.errorListener(InlineErrorListener(runtime, FileLineIndex(content), start))
            }

            var next: AntlrToken
            val stream = InlineTokenStream(scanner, runtime.input, start, runtime.at(start))
            while (stream.LT(1).also { next = it }.type != AntlrToken.EOF) {
                tokens += next; stream.consume()
            }
        }

        else -> error("Illegal block type ${block::class.simpleName}")
    }
}

private fun List<BlockToken>.isCommented(): Boolean =
    all { it is CommentBlock || (it is ContentBlock && it.isBlank()) }

private fun processTemplate(replacement: String, tokens: List<LeafToken>): String {
    if (tokens.isEmpty()) return replacement

    // Collect insertable values
    val arguments = tokens.map {
        if (it.type.value == StitcherLexer.QUOTED) it.text.substring(1, it.range.last) else it.text
    }
    // Collect insertable tokens
    val places = SwapTemplate(replacement.toStream()).run {
        generateSequence { nextToken().takeUnless(Token::isEOF) }.map(Token::range).toList()
    }

    return buildString(replacement) {
        for (range in places.asReversed()) {
            // FIXME: Use checked conversion and list getter
            val index = substring(range).substring(1).toInt()
            val value = arguments[index - 1]
            replace(range.first, range.last + 1, value)
        }
    }
}