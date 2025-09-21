package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.issue.*
import dev.kikugie.stitcher.transform.TransformParameters

internal class ExpressionEvaluator(val parameters: TransformParameters) : ExpressionToken.Visitor<Boolean> {
    override fun visitGroup(it: ExpressionToken.Group): Boolean = it.body.accept(this)

    override fun visitUnary(it: ExpressionToken.Unary): Boolean = when (it.operator.type) {
        StitcherParser.OP_NOT -> !it.operand.accept(this)
        else -> parameters.sink.at(it.operator) bail problem { "Unsupported unary operator ${it.operator.name}" }
    }

    override fun visitBinary(it: ExpressionToken.Binary): Boolean = when (it.operator.type) {
        StitcherParser.OP_AND -> it.left.accept(this) && it.right.accept(this)
        StitcherParser.OP_OR -> it.left.accept(this) || it.right.accept(this)
        else -> parameters.sink.at(it.operator) bail problem { "Unsupported binary operator ${it.operator.name}" }
    }

    override fun visitConstant(it: ExpressionToken.Constant): Boolean = parameters.sink.verifyNotNull(parameters.constants[it.value.text]) {
        at(it.value) bail problem { "Unresolved constant '${it.value.text}'" }
    }

    override fun visitAssignment(it: ExpressionToken.Assignment): Boolean {
        val target = parameters.sink.verifyNotNull(parameters.dependencies[it.target?.text.orEmpty()]) {
            val target = it.target
            if (target != null) at(target) bail problem { "Unresolved dependency '${target.text}'" }
            else at(it.predicates.first()) bail problem { "No default dependency specified" }
        }
        return it.predicates.all { it.comparator(target, it.version) }
    }
}