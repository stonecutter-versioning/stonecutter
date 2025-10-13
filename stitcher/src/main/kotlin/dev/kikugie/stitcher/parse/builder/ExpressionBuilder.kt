package dev.kikugie.stitcher.parse.builder

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.antlr.InlineTokenConverter
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report

internal class ExpressionBuilder(val sink: ProblemSink, val converter: InlineTokenConverter) : StitcherBaseVisitor<ExpressionToken>() {
    private val predicateBuilder: PredicateBuilder = PredicateBuilder(sink, converter)

    override fun visitConditionExpression(ctx: StitcherParser.ConditionExpressionContext): ExpressionToken {
        ctx.LEFT_BRACE()?.let {
            val body = ctx.conditionExpression(0)
                .accept(this)
            return ExpressionToken.Group(converter(it), body, converter(ctx.RIGHT_BRACE()))
        }

        ctx.OP_NOT()?.let {
            val body = ctx.conditionExpression(0)
                .accept(this)
            return ExpressionToken.Unary(converter(it), body)
        }

        (ctx.OP_AND() ?: ctx.OP_OR())?.let {
            val left = ctx.conditionExpression(0)
                .accept(this)
            val right = ctx.conditionExpression(1)
                .accept(this)
            return ExpressionToken.Binary(left, converter(it), right)
        }

        return ctx.endpointExpression()?.accept(this)!!
    }

    override fun visitEndpointExpression(ctx: StitcherParser.EndpointExpressionContext): ExpressionToken {
        val identifier = ctx.IDENTIFIER()?.let(converter::invoke)
        val operator = ctx.OP_ASSIGN()?.let(converter::invoke)
        val predicates = ctx.versionPredicate().mapNotNull {
            it.runCatching { accept(predicateBuilder) }.getOrElse { e ->
                sink.at(it.start) report problem(e) { "Failed to parse version predicate" }; null
            }
        }

        return when {
            identifier != null && operator == null -> ExpressionToken.Constant(identifier)
            identifier == null -> ExpressionToken.Assignment(predicates,)
            else -> ExpressionToken.Assignment(identifier, operator!!, predicates,)
        }
    }
}