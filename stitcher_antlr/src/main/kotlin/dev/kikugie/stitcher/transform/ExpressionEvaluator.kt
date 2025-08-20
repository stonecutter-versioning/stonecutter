package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.api.TransformParameters
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.util.unsupported
import dev.kikugie.stitcher.util.verify

internal class ExpressionEvaluator(val parameters: TransformParameters) : ExpressionToken.Visitor<Boolean> {
    override fun visitGroup(it: ExpressionToken.Group): Boolean = it.body.accept(this)

    override fun visitUnary(it: ExpressionToken.Unary): Boolean = when (it.operator.type) {
        StitcherParser.OP_NOT -> !it.operand.accept(this)
        else -> unsupported("Unsupported unary operator ${it.operator.typeName}")
    }

    override fun visitBinary(it: ExpressionToken.Binary): Boolean = when (it.operator.type) {
        StitcherParser.OP_AND -> it.left.accept(this) && it.right.accept(this)
        StitcherParser.OP_OR -> it.left.accept(this) || it.right.accept(this)
        else -> unsupported("Unsupported binary operator ${it.operator.typeName}")
    }

    override fun visitConstant(it: ExpressionToken.Constant): Boolean {
        val value = it.value.text
        return verify(parameters.constants[value]) { "Undefined constant '$value'" }
    }

    override fun visitAssignment(it: ExpressionToken.Assignment): Boolean {
        val value = it.target?.text.orEmpty()
        val target = verify(parameters.dependencies[value]) { "Undefined dependency '$value'" }
        return it.predicates.all { it.comparator(target, it.version) }
    }
}