package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.data.composite.AssignmentExpression
import dev.kikugie.stitcher.data.composite.BinaryExpression
import dev.kikugie.stitcher.data.composite.ConstantExpression
import dev.kikugie.stitcher.data.composite.ExpressionToken
import dev.kikugie.stitcher.data.composite.GroupExpression
import dev.kikugie.stitcher.data.composite.UnaryExpression
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.bail
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.verifyNotNull
import dev.kikugie.stitcher.transform.RuntimeState
import dev.kikugie.stitcher.transform.TransformParameters

internal class ExpressionEvaluator(val runtime: RuntimeState, val parameters: TransformParameters) : ExpressionToken.Visitor<Boolean> {
    override fun visitGroup(group: GroupExpression): Boolean = group.body.accept(this)
    override fun visitUnary(unary: UnaryExpression): Boolean = when (unary.operator.type.value) {
        StitcherLexer.OP_NOT -> !unary.target.accept(this)
        else -> runtime.sink.at(unary.operator) bail problem { "Unsupported unary operator ${unary.operator.type.name}" }
    }

    override fun visitBinary(binary: BinaryExpression): Boolean = when (binary.operator.type.value) {
        StitcherLexer.OP_AND -> binary.left.accept(this) && binary.right.accept(this)
        StitcherLexer.OP_OR -> binary.left.accept(this) || binary.right.accept(this)
        else -> runtime.sink.at(binary.operator) bail problem { "Unsupported binary operator ${binary.operator.type.name}" }
    }

    override fun visitConstant(constant: ConstantExpression): Boolean = runtime.sink.verifyNotNull(parameters.constants[constant.value.text]) {
        at(constant.value) bail problem { "Unresolved constant '${constant.value.text}'" }
    }

    override fun visitAssignment(assignment: AssignmentExpression): Boolean {
        val target = runtime.sink.verifyNotNull(parameters.dependencies[assignment.target?.text.orEmpty()]) {
            val target = assignment.target
            if (target != null) at(target) bail problem { "Unresolved dependency '${target.text}'" }
            else at(assignment.predicates.first()) bail problem { "No default dependency specified" }
        }
        return assignment.predicates.all { it.predicate(target) }
    }
}