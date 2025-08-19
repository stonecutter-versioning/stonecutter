package dev.kikugie.stitcher.antlr.converter

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.util.toLeaf
import org.antlr.v4.runtime.tree.TerminalNode

internal object DefinitionBuilder : StitcherBaseVisitor<DefinitionToken>() {
    override fun visitReplacement(ctx: StitcherParser.ReplacementContext): DefinitionToken =
        DefinitionToken.Replacement(ctx.IDENTIFIER().toLeaf())

    override fun visitSwap(ctx: StitcherParser.SwapContext): DefinitionToken {
        ctx.SCOPE_CLOSE()?.let {
            val closer = it.toLeaf()
            return DefinitionToken.Swap(closer)
        }

        val identifier = ctx.IDENTIFIER().toLeaf()
        val arguments = ctx.swapArguments().mapToLeaves()
        val opener = ctx.scopeOpener()?.toLeaf()
        return DefinitionToken.Swap(identifier, arguments, opener)
    }

    override fun visitCondition(ctx: StitcherParser.ConditionContext): DefinitionToken {
        val closer = ctx.SCOPE_CLOSE()?.toLeaf()
        val opener = ctx.scopeOpener()?.toLeaf()
        val sugar = listOfNotNull(ctx.SUGAR_IF(), ctx.SUGAR_ELIF(), ctx.SUGAR_ELSE())
            .map(TerminalNode::toLeaf)
        val expression = ctx.conditionExpression()
            ?.accept(ExpressionBuilder)

        return when {
            closer != null && opener == null && expression == null && sugar.isEmpty() -> DefinitionToken.Condition(closer)
            closer == null -> DefinitionToken.Condition(sugar, expression!!, opener)
            else -> DefinitionToken.Condition(closer, sugar, expression, opener)
        }
    }

    private fun StitcherParser.ScopeOpenerContext.toLeaf(): LeafToken = (SCOPE_OPEN()?: SCOPE_WORD()).toLeaf()

    private fun StitcherParser.SwapArgumentsContext.mapToLeaves(): List<LeafToken> = children.orEmpty().mapNotNull {
        val node = it as? TerminalNode ?: return@mapNotNull null
        when(node.symbol.type) {
            StitcherParser.IDENTIFIER -> node.toLeaf()
            StitcherParser.QUOTED -> node.toLeaf()
            else -> null
        }
    }
}