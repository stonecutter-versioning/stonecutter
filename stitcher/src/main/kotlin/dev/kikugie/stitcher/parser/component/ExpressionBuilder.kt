package dev.kikugie.stitcher.parser.component

import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.composite.*
import dev.kikugie.stitcher.issue.*
import dev.kikugie.stitcher.parser.StitcherTokenFactory

internal class ExpressionBuilder(val problems: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<ExpressionToken>() {
    private val predicateBuilder: PredicateBuilder by lazy { PredicateBuilder(problems, factory) }

    override fun visitBinaryExpression(ctx: StitcherParser.BinaryExpressionContext): BinaryExpression {
        val operator = factory.fromAntlrToken(ctx.op)
        val left = ctx.conditionExpression(0).accept(this)
        val right = ctx.conditionExpression(1).accept(this)
        return BinaryExpression(left, operator, right)
    }

    override fun visitUnaryExpression(ctx: StitcherParser.UnaryExpressionContext): UnaryExpression {
        val operator = factory.fromAntlrNode(ctx.OP_NOT())
        val target = ctx.conditionExpression().accept(this)
        return UnaryExpression(operator, target)
    }

    override fun visitGroupExpression(ctx: StitcherParser.GroupExpressionContext): GroupExpression {
        val lb = factory.fromAntlrNode(ctx.LEFT_BRACE())
        val rb = factory.fromAntlrNode(ctx.RIGHT_BRACE())
        val body = ctx.conditionExpression().accept(this)
        return GroupExpression(lb, body, rb)
    }

    override fun visitAssignmentExpression(ctx: StitcherParser.AssignmentExpressionContext): AssignmentExpression {
        val identifier = ctx.IDENTIFIER()?.let(factory::fromAntlrNode)
        val operator = ctx.OP_ASSIGN()?.let(factory::fromAntlrNode)
        val predicates = ctx.versionPredicate().mapNotNull(::resolve)
        return if (identifier == null || operator == null) AssignmentExpression(predicates)
        else AssignmentExpression(identifier, operator, predicates)
    }

    override fun visitConstantExpression(ctx: StitcherParser.ConstantExpressionContext): ConstantExpression {
        val identifier = factory.fromAntlrNode(ctx.IDENTIFIER())
        return ConstantExpression(identifier)
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