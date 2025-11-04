package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.data.composite.AssignmentExpression
import dev.kikugie.stitcher.data.composite.BinaryExpression
import dev.kikugie.stitcher.data.composite.ConstantExpression
import dev.kikugie.stitcher.data.composite.ExpressionToken
import dev.kikugie.stitcher.data.composite.GroupExpression
import dev.kikugie.stitcher.data.composite.UnaryExpression
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.transform.RuntimeState
import dev.kikugie.stitcher.transform.TransformParameters

internal class ExpressionEvaluator(val runtime: RuntimeState, val parameters: TransformParameters) : ExpressionToken.Visitor<Boolean>, ProblemSource by runtime {
    override fun visitGroup(group: GroupExpression): Boolean = group.body.accept(this)
    override fun visitUnary(unary: UnaryExpression): Boolean = when (unary.operator.type.value) {
        StitcherLexer.OP_NOT -> !unary.target.accept(this)
        else -> at(unary.operator) bail "Unsupported unary operator ${unary.operator.type.name}"
    }

    override fun visitBinary(binary: BinaryExpression): Boolean = when (binary.operator.type.value) {
        StitcherLexer.OP_AND -> binary.left.accept(this) && binary.right.accept(this)
        StitcherLexer.OP_OR -> binary.left.accept(this) || binary.right.accept(this)
        else -> at(binary.operator) bail "Unsupported unary operator ${binary.operator.type.name}"
    }

    override fun visitConstant(constant: ConstantExpression): Boolean {
        val value = parameters.constants[constant.value.text]
        return value ?: (at(constant.value) bail "Unresolved constant '${constant.value.text}'")
    }

    override fun visitAssignment(assignment: AssignmentExpression): Boolean {
        val value = parameters.dependencies[assignment.target?.text.orEmpty()]
        if (value == null)
            if (assignment.target != null) at(assignment.target) bail "Unresolved dependency '${assignment.target.text}'"
            else at(assignment.predicates.first()) bail "No default dependency specified"
        return assignment.predicates.all { it.predicate(value) }
    }
}