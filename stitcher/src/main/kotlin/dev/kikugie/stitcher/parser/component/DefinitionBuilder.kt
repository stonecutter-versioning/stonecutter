package dev.kikugie.stitcher.parser.component

import dev.kikugie.commons.takeAsOrNull
import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.StitcherVisitor
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.bail
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import kotlin.collections.orEmpty

internal class DefinitionBuilder(val problems: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<DefinitionToken>() {
    private val expressionBuilder: ExpressionBuilder by lazy { ExpressionBuilder(problems, factory) }

    override fun visitReplacement(ctx: StitcherParser.ReplacementContext): DefinitionToken =
        DefinitionToken.Replacement(factory.fromAntlrNode(ctx.IDENTIFIER()))

    override fun visitOpenerSwap(ctx: StitcherParser.OpenerSwapContext): DefinitionToken {
        val identifier = factory.fromAntlrNode(ctx.IDENTIFIER())
        val arguments = ctx.swapArguments()?.children.orEmpty<ParseTree>().asSequence()
            .mapNotNull { it.takeAsOrNull<TerminalNode>()?.symbol }
            .filter { it.type == StitcherLexer.IDENTIFIER || it.type == StitcherLexer.QUOTED }
            .map { factory.fromAntlrToken(it) }
        val opener = ctx.scopeOpener()?.op?.let(factory::fromAntlrToken)
        return DefinitionToken.Swap(identifier, arguments.toList(), opener)
    }

    override fun visitCloserSwap(ctx: StitcherParser.CloserSwapContext): DefinitionToken {
        val closer = factory.fromAntlrNode(ctx.SCOPE_CLOSE())
        return DefinitionToken.Swap(closer)
    }

    override fun visitOpenerCondition(ctx: StitcherParser.OpenerConditionContext): DefinitionToken {
        val sugar = listOfNotNull(ctx.SUGAR_IF()?.let(factory::fromAntlrNode))
        val expression = ctx.conditionExpression().runCatching { accept(expressionBuilder) }.getOrElse {
            problems.at(ctx.start) bail problem(it) { "Failed to parse expression" }
        }
        val opener = ctx.scopeOpener()?.op?.let(factory::fromAntlrToken)
        return DefinitionToken.Condition(sugar, expression, opener)
    }

    override fun visitExtensionCondition(ctx: StitcherParser.ExtensionConditionContext): DefinitionToken {
        val closer = ctx.SCOPE_CLOSE().let(factory::fromAntlrNode)
        val sugar = listOfNotNull(ctx.SUGAR_ELIF(), ctx.SUGAR_ELSE(), ctx.SUGAR_IF())
            .map(factory::fromAntlrNode)
        val expression = ctx.conditionExpression()?.runCatching { accept(expressionBuilder) }?.getOrElse {
            problems.at(ctx.start) bail problem(it) { "Failed to parse expression" }
        }
        val opener = ctx.scopeOpener()?.op?.let(factory::fromAntlrToken)
        return DefinitionToken.Condition(closer, sugar, expression, opener)
    }

    override fun visitCloserCondition(ctx: StitcherParser.CloserConditionContext): DefinitionToken {
        val closer = ctx.SCOPE_CLOSE().let(factory::fromAntlrNode)
        return DefinitionToken.Condition(closer)
    }

    private class PairDefinitionBuilder(problems: ProblemSink, factory: StitcherTokenFactory) : StitcherBaseVisitor<Pair<AntlrToken, DefinitionToken>>() {
        private val definitionBuilder = DefinitionBuilder(problems, factory)

        override fun visitConditionDefinition(ctx: StitcherParser.ConditionDefinitionContext): Pair<AntlrToken, DefinitionToken> =
            ctx.COND_MARK().symbol to ctx.condition().accept(definitionBuilder)

        override fun visitSwapDefinition(ctx: StitcherParser.SwapDefinitionContext): Pair<AntlrToken, DefinitionToken> =
            ctx.SWAP_MARK().symbol to ctx.swap().accept(definitionBuilder)

        override fun visitReplacementDefinition(ctx: StitcherParser.ReplacementDefinitionContext): Pair<AntlrToken, DefinitionToken> =
            ctx.REPL_MARK().symbol to ctx.replacement().accept(definitionBuilder)
    }

    companion object {
        fun paired(problems: ProblemSink, factory: StitcherTokenFactory): StitcherVisitor<Pair<AntlrToken, DefinitionToken>> =
            PairDefinitionBuilder(problems, factory)
    }
}