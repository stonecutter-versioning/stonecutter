package dev.kikugie.stitcher.antlr.converter

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.util.toLeaf

object ExpressionBuilder : StitcherBaseVisitor<ExpressionToken>() {
    override fun visitConditionExpression(ctx: StitcherParser.ConditionExpressionContext): ExpressionToken {
        ctx.LEFT_BRACE()?.let {
            val lb = it.toLeaf()
            val rb = ctx.RIGHT_BRACE().toLeaf()
            val body = ctx.conditionExpression(0)
                .accept(this)

            return ExpressionToken.Group(lb, body, rb)
        }

        ctx.OP_NOT()?.let {
            val op = it.toLeaf()
            val body = ctx.conditionExpression(0)
                .accept(this)

            return ExpressionToken.Unary(op, body)
        }

        (ctx.OP_AND() ?: ctx.OP_OR())?.let {
            val op = it.toLeaf()
            val left = ctx.conditionExpression(0)
                .accept(this)
            val right = ctx.conditionExpression(1)
                .accept(this)

            return ExpressionToken.Binary(left, op, right)
        }

        return ctx.endpointExpression()?.accept(this)!!
    }

    override fun visitEndpointExpression(ctx: StitcherParser.EndpointExpressionContext): ExpressionToken {
        val identifier = ctx.IDENTIFIER()?.toLeaf()
        val operator = ctx.OP_ASSIGN()?.toLeaf()
        val predicates = ctx.versionPredicate()
            .map { it.accept(PredicateBuilder) }

        return when {
            identifier != null && operator == null -> ExpressionToken.Constant(identifier)
            identifier == null -> ExpressionToken.Assignment(predicates,)
            else -> ExpressionToken.Assignment(identifier, operator!!, predicates,)
        }
    }
}