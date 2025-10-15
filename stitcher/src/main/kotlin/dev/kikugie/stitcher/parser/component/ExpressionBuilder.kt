package dev.kikugie.stitcher.parser.component

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.issue.*
import dev.kikugie.stitcher.parser.StitcherTokenFactory

internal class ExpressionBuilder(val problems: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<ExpressionToken>() {
    private val predicateBuilder: PredicateBuilder by lazy { PredicateBuilder(problems, factory) }

    override fun visitBinaryExpression(ctx: StitcherParser.BinaryExpressionContext): ExpressionToken {
        val operator = factory.fromAntlrToken(ctx.op)
        val left = ctx.conditionExpression(0).accept(this)
        val right = ctx.conditionExpression(1).accept(this)
        return ExpressionToken.Binary(left, operator, right)
    }

    override fun visitUnaryExpression(ctx: StitcherParser.UnaryExpressionContext): ExpressionToken {
        val operator = factory.fromAntlrNode(ctx.OP_NOT())
        val target = ctx.conditionExpression().accept(this)
        return ExpressionToken.Unary(operator, target)
    }

    override fun visitGroupExpression(ctx: StitcherParser.GroupExpressionContext): ExpressionToken {
        val lb = factory.fromAntlrNode(ctx.LEFT_BRACE())
        val rb = factory.fromAntlrNode(ctx.RIGHT_BRACE())
        val body = ctx.conditionExpression().accept(this)
        return ExpressionToken.Group(lb, body, rb)
    }

    override fun visitAssignmentExpression(ctx: StitcherParser.AssignmentExpressionContext): ExpressionToken {
        val identifier = ctx.IDENTIFIER()?.let(factory::fromAntlrNode)
        val operator = ctx.OP_ASSIGN()?.let(factory::fromAntlrNode)
        val predicates = ctx.versionPredicate().mapNotNull(::resolve)
        return if (identifier == null || operator == null) ExpressionToken.Assignment(predicates)
        else ExpressionToken.Assignment(identifier, operator, predicates)
    }

    override fun visitConstantExpression(ctx: StitcherParser.ConstantExpressionContext): ExpressionToken {
        val identifier = factory.fromAntlrNode(ctx.IDENTIFIER())
        return ExpressionToken.Constant(identifier)
    }

    private fun resolve(ctx: StitcherParser.VersionPredicateContext) = try {
        ctx.accept(predicateBuilder)
    } catch (_: BailException) {
        null
    } catch (e: Throwable) {
        problems.at(ctx.start) report problem(e) { "Failed to parse version predicate" }
        null
    }
}