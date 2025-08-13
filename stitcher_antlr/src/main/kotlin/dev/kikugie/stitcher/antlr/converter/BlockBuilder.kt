package dev.kikugie.stitcher.antlr.converter

import dev.kikugie.stitcher.antlr.LayoutBaseVisitor
import dev.kikugie.stitcher.antlr.LayoutParser
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.adapter.InlineTokenFactory
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.util.range
import dev.kikugie.stitcher.util.toLeaf
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

class BlockBuilder(private val scope: MutableList<BlockToken> = mutableListOf()) : LayoutBaseVisitor<Unit>() {
    companion object : LayoutBaseVisitor<BlockToken.Root>() {
        override fun visitFile(ctx: LayoutParser.FileContext) = BlockToken.Root(buildList {
            ctx.accept(BlockBuilder(this))
        })
    }

    override fun visitFile(ctx: LayoutParser.FileContext) {
        for (it in ctx.block()) it.accept(this)
    }

    override fun visitBlock(ctx: LayoutParser.BlockContext) {
        ctx.children.single().accept(this)
    }

    override fun visitContent(ctx: LayoutParser.ContentContext) {
        scope += plain(ctx.CONTENT())
    }

    override fun visitComment(ctx: LayoutParser.CommentContext) {
        scope += BlockToken.Comment(plain(ctx.COMMENT_BODY()), ctx.range)
    }

    override fun visitSwap(ctx: LayoutParser.SwapContext) {
        if (ctx.swapFreeOpener()?.accept(this) != null) with(scope.last() as BlockToken.Code) {
            scope = populate(ctx.block())
            return
        }

        if (ctx.swapOpener()?.accept(this) != null) with(scope.last() as BlockToken.Code) {
            scope = populate(ctx.block())
            return
        }

        ctx.swapCloser()?.accept(this)
    }

    override fun visitCondition(ctx: LayoutParser.ConditionContext) {
        if (ctx.conditionFreeOpener()?.accept(this) != null) with(scope.last() as BlockToken.Code) {
            scope = populate(ctx.block())
            return
        }

        if (ctx.conditionOpener()?.accept(this) != null) with(scope.last() as BlockToken.Code) {
            scope = populate(ctx.block())
            ctx.extension()?.accept(this@BlockBuilder)
            return
        }
    }

    override fun visitExtension(ctx: LayoutParser.ExtensionContext) {
        if (ctx.conditionFreeExtension()?.accept(this) != null) with(scope.last() as BlockToken.Code) {
            scope = populate(ctx.block())
            return
        }

        if (ctx.conditionExtension()?.accept(this) != null) with(scope.last() as BlockToken.Code) {
            scope = populate(ctx.block())
            ctx.extension()?.accept(this@BlockBuilder)
            return
        }

        ctx.conditionCloser()?.accept(this)
    }

    override fun visitReplacement(ctx: LayoutParser.ReplacementContext) = parse(ctx.REPLACEMENT())
    override fun visitSwapOpener(ctx: LayoutParser.SwapOpenerContext) = parse(ctx.SWAP_OPENER())
    override fun visitSwapFreeOpener(ctx: LayoutParser.SwapFreeOpenerContext) = parse(ctx.SWAP_FREE_OPENER())
    override fun visitSwapCloser(ctx: LayoutParser.SwapCloserContext) = parse(ctx.SWAP_CLOSER())
    override fun visitConditionOpener(ctx: LayoutParser.ConditionOpenerContext) = parse(ctx.CONDITION_OPENER())
    override fun visitConditionFreeOpener(ctx: LayoutParser.ConditionFreeOpenerContext) = parse(ctx.CONDITION_FREE_OPENER())
    override fun visitConditionExtension(ctx: LayoutParser.ConditionExtensionContext) = parse(ctx.CONDITION_EXTENSION())
    override fun visitConditionFreeExtension(ctx: LayoutParser.ConditionFreeExtensionContext)= parse(ctx.CONDITION_FREE_EXTENSION())
    override fun visitConditionCloser(ctx: LayoutParser.ConditionCloserContext) = parse(ctx.CONDITION_CLOSER())

    private fun populate(blocks: List<ParserRuleContext>): List<BlockToken> = buildList {
        val visitor = BlockBuilder(this)
        for (it in blocks) it.accept(visitor)
    }
    private fun plain(node: TerminalNode): BlockToken.Content =
        BlockToken.Content(node.range, node.symbol.inputStream)
    private fun parse(node: TerminalNode) {
        val token = node.symbol
        val lexer = StitcherLexer(CharStreams.fromString(token.text)).apply {
            tokenFactory = InlineTokenFactory(this, token.inputStream, token.startIndex, token.line, token.charPositionInLine)
        }
        val parser = StitcherParser(CommonTokenStream(lexer))
        val definition = parser.definition()
        scope += definition.resolve()
    }

    private fun StitcherParser.DefinitionContext.resolve(): BlockToken.Code {
        var marker: TerminalNode
        var definition: ParserRuleContext

        when {
            COND_MARK() != null -> {
                marker = COND_MARK()
                definition = condition()
            }

            SWAP_MARK() != null -> {
                marker = SWAP_MARK()
                definition = swap()
            }

            REPL_MARK() != null -> {
                marker = REPL_MARK()
                definition = replacement()
            }

            else -> error("$this has no definition")
        }

        return BlockToken.Code(marker.toLeaf(), definition.accept(DefinitionBuilder))
    }
}