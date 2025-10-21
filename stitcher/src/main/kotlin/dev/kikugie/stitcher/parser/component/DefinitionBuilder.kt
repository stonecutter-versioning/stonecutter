package dev.kikugie.stitcher.parser.component

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.StitcherVisitor
import dev.kikugie.stitcher.data.composite.*
import dev.kikugie.stitcher.data.custom.ClosedScope
import dev.kikugie.stitcher.data.custom.ScopeToken
import dev.kikugie.stitcher.data.custom.WordScope
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.bail
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.util.AntlrToken

internal class DefinitionBuilder(val problems: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<DefinitionToken>() {
    private val expressionBuilder: ExpressionBuilder by lazy { ExpressionBuilder(problems, factory) }
    private val scopeTokenBuilder: ScopeTokenBuilder by lazy { ScopeTokenBuilder(problems, factory) }

    override fun visitReplacement(ctx: StitcherParser.ReplacementContext): DefinitionToken =
        ReplacementDefinition(factory.fromAntlrNode(ctx.IDENTIFIER()))

    override fun visitOpenerSwap(ctx: StitcherParser.OpenerSwapContext): DefinitionToken {
        val identifier = factory.fromAntlrNode(ctx.IDENTIFIER())
        val arguments = ctx.swapArguments()?.literal().orEmpty<StitcherParser.LiteralContext>()
            .map { factory.fromAntlrToken(it.start) }
        val opener = ctx.scopeOpener()?.accept(scopeTokenBuilder)
        return SwapDefinition.Opener(identifier, arguments.toList(), opener)
    }

    override fun visitCloserSwap(ctx: StitcherParser.CloserSwapContext): DefinitionToken {
        val closer = factory.fromAntlrNode(ctx.SCOPE_CLOSE())
        return SwapDefinition.Closer(closer)
    }

    override fun visitOpenerCondition(ctx: StitcherParser.OpenerConditionContext): DefinitionToken {
        val sugar = listOfNotNull(ctx.SUGAR_IF()?.let(factory::fromAntlrNode))
        val expression = ctx.conditionExpression().runCatching { accept(expressionBuilder) }.getOrElse {
            problems.at(ctx.start) bail problem(it) { "Failed to parse expression" }
        }
        val opener = ctx.scopeOpener()?.accept(scopeTokenBuilder)
        return ConditionDefinition.Opener(sugar, expression, opener)
    }

    override fun visitExtensionCondition(ctx: StitcherParser.ExtensionConditionContext): DefinitionToken {
        val closer = ctx.SCOPE_CLOSE().let(factory::fromAntlrNode)
        val sugar = listOfNotNull(ctx.SUGAR_ELIF(), ctx.SUGAR_ELSE(), ctx.SUGAR_IF())
            .map(factory::fromAntlrNode)
        val expression = ctx.conditionExpression()?.runCatching { accept(expressionBuilder) }?.getOrElse {
            problems.at(ctx.start) bail problem(it) { "Failed to parse expression" }
        }
        val opener = ctx.scopeOpener()?.accept(scopeTokenBuilder)
        return ConditionDefinition.Extension(closer, sugar, expression, opener)
    }

    override fun visitCloserCondition(ctx: StitcherParser.CloserConditionContext): DefinitionToken {
        val closer = ctx.SCOPE_CLOSE().let(factory::fromAntlrNode)
        return ConditionDefinition.Closer(closer)
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

    private class ScopeTokenBuilder(val problems: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<ScopeToken>() {
        override fun visitClosedScopeOpener(ctx: StitcherParser.ClosedScopeOpenerContext): ScopeToken = ClosedScope(
            ctx.SCOPE_OPEN().let(factory::fromAntlrNode)
        )

        override fun visitWordScopeOpener(ctx: StitcherParser.WordScopeOpenerContext): ScopeToken = WordScope(
            ctx.SCOPE_WORD().let(factory::fromAntlrNode),
            ctx.PLUS()?.let(factory::fromAntlrNode),
            ctx.literal()?.start?.let(factory::fromAntlrToken)
        )
    }

    companion object {
        fun paired(problems: ProblemSink, factory: StitcherTokenFactory): StitcherVisitor<Pair<AntlrToken, DefinitionToken>> =
            PairDefinitionBuilder(problems, factory)
    }
}