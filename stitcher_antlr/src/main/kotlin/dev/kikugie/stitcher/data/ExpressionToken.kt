package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.util.merge
import org.antlr.v4.runtime.CharStream

internal sealed interface ExpressionToken : StitcherToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitGroup(it: Group): T
        fun visitUnary(it: Unary): T
        fun visitBinary(it: Binary): T
        fun visitConstant(it: Constant): T
        fun visitAssignment(it: Assignment): T
    }

    data class Group(val lb: LeafToken, val body: ExpressionToken, val rb: LeafToken) : ExpressionToken {
        override val range: IntRange get() = merge(lb.range, body.range, rb.range)
        override val source: CharStream get() = lb.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitGroup(this)
    }

    data class Unary(val operator: LeafToken, val operand: ExpressionToken) : ExpressionToken {
        override val range: IntRange get() = merge(operator.range, operand.range)
        override val source: CharStream get() = operator.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitUnary(this)
    }

    data class Binary(val left: ExpressionToken, val operator: LeafToken, val right: ExpressionToken
    ) : ExpressionToken {
        override val range: IntRange get() = merge(left.range, operator.range, right.range)
        override val source: CharStream get() = operator.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitBinary(this)
    }

    data class Constant(val value: LeafToken) : ExpressionToken {
        override val range: IntRange get() = value.range
        override val source: CharStream get() = value.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitConstant(this)
    }

    sealed interface Assignment : ExpressionToken {
        val target: LeafToken? get() = null
        val operator: LeafToken? get() = null
        val predicates: List<PredicateToken>

        data class Implicit(override val predicates: List<PredicateToken>) : Assignment {
            override val range: IntRange get() = merge(predicates.firstOrNull()?.range, predicates.lastOrNull()?.range)
            override val source: CharStream get() = predicates.first().source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitAssignment(this)
        }

        data class Explicit(override val target: LeafToken, override val operator: LeafToken, override val predicates: List<PredicateToken>) : Assignment {
            override val range: IntRange get() = merge(target.range, operator.range, predicates.lastOrNull()?.range)
            override val source: CharStream get() = target.source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitAssignment(this)
        }

        companion object {
            operator fun invoke(predicates: List<PredicateToken>): Assignment =
                Implicit(predicates)
            operator fun invoke(target: LeafToken, operator: LeafToken, predicates: List<PredicateToken>): Assignment =
                Explicit(target, operator, predicates)
        }
    }
}