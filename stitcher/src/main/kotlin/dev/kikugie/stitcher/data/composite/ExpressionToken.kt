package dev.kikugie.stitcher.data.composite

import dev.kikugie.stitcher.data.custom.PredicateToken
import dev.kikugie.stitcher.data.leaf.LeafToken

internal sealed interface ExpressionToken {
    fun <T> accept(visitor: Visitor<T>): T
    
    interface Visitor<T> {
        fun visitGroup(group: GroupExpression): T
        fun visitUnary(unary: UnaryExpression): T
        fun visitBinary(binary: BinaryExpression): T
        fun visitConstant(constant: ConstantExpression): T
        fun visitAssignment(assignment: AssignmentExpression): T
    }
}

internal data class GroupExpression(val lb: LeafToken, val body: ExpressionToken, val rb: LeafToken) : ExpressionToken {
    override fun <T> accept(visitor: ExpressionToken.Visitor<T>): T = visitor.visitGroup(this)
}

internal data class UnaryExpression(val operator: LeafToken, val target: ExpressionToken) : ExpressionToken {
    override fun <T> accept(visitor: ExpressionToken.Visitor<T>): T = visitor.visitUnary(this)
}

internal data class BinaryExpression(val left: ExpressionToken, val operator: LeafToken, val right: ExpressionToken) : ExpressionToken {
    override fun <T> accept(visitor: ExpressionToken.Visitor<T>): T = visitor.visitBinary(this)
}

internal data class ConstantExpression(val value: LeafToken) : ExpressionToken {
    override fun <T> accept(visitor: ExpressionToken.Visitor<T>): T = visitor.visitConstant(this)
}

internal data class AssignmentExpression(val target: LeafToken?, val operator: LeafToken?, val predicates: List<PredicateToken>) : ExpressionToken {
    constructor(predicates: List<PredicateToken>) : this(null, null, predicates)
    override fun <T> accept(visitor: ExpressionToken.Visitor<T>): T = visitor.visitAssignment(this)
}