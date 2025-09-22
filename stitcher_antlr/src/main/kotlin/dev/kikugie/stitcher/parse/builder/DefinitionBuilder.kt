package dev.kikugie.stitcher.parse.builder

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.parse.inline.InlineTokenConverter
import org.antlr.v4.runtime.tree.TerminalNode

internal class DefinitionBuilder(sink: ProblemSink, val converter: InlineTokenConverter) : StitcherBaseVisitor<DefinitionToken>() {
    private val expressionBuilder: ExpressionBuilder = ExpressionBuilder(sink, converter)
    override fun visitReplacement(ctx: StitcherParser.ReplacementContext): DefinitionToken =
        DefinitionToken.Replacement(converter(ctx.IDENTIFIER()))

    override fun visitSwap(ctx: StitcherParser.SwapContext): DefinitionToken {
        ctx.SCOPE_CLOSE()?.let {
            return DefinitionToken.Swap(converter(it))
        }

        val arguments = ctx.swapArguments().mapToLeaves()
        val opener = ctx.scopeOpener()?.start?.let(converter::invoke)
        return DefinitionToken.Swap(converter(ctx.IDENTIFIER()), arguments, opener)
    }

    override fun visitCondition(ctx: StitcherParser.ConditionContext): DefinitionToken {
        val closer = ctx.SCOPE_CLOSE()?.let(converter::invoke)
        val opener = ctx.scopeOpener()?.start?.let(converter::invoke)
        val sugar = listOfNotNull(ctx.SUGAR_IF(), ctx.SUGAR_ELIF(), ctx.SUGAR_ELSE())
            .map(converter::invoke)
        val expression = ctx.conditionExpression()
            ?.accept(expressionBuilder)

        return when {
            closer != null && opener == null && expression == null && sugar.isEmpty() -> DefinitionToken.Condition(closer)
            closer == null -> DefinitionToken.Condition(sugar, expression!!, opener)
            else -> DefinitionToken.Condition(closer, sugar, expression, opener)
        }
    }

    private fun StitcherParser.SwapArgumentsContext.mapToLeaves(): List<LeafToken> = children.orEmpty().mapNotNull {
        val node = it as? TerminalNode ?: return@mapNotNull null
        when(node.symbol.type) {
            StitcherParser.IDENTIFIER -> converter(node)
            StitcherParser.QUOTED -> converter(node)
            else -> null
        }
    }
}