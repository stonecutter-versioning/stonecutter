package dev.kikugie.stitcher.data

internal sealed interface ExpressionToken {
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitGroup(it: Group): T
        fun visitUnary(it: Unary): T
        fun visitBinary(it: Binary): T
        fun visitConstant(it: Constant): T
        fun visitAssignment(it: Assignment): T
    }

    data class Group(val lb: LeafToken, val body: ExpressionToken, val rb: LeafToken) : ExpressionToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitGroup(this)
    }

    data class Unary(val operator: LeafToken, val operand: ExpressionToken) : ExpressionToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitUnary(this)
    }

    data class Binary(val left: ExpressionToken, val operator: LeafToken, val right: ExpressionToken) : ExpressionToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitBinary(this)
    }

    data class Constant(val value: LeafToken) : ExpressionToken {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitConstant(this)
    }

    sealed interface Assignment : ExpressionToken {
        val target: LeafToken? get() = null
        val operator: LeafToken? get() = null
        val predicates: List<PredicateToken>

        data class Implicit(override val predicates: List<PredicateToken>) : Assignment {
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitAssignment(this)
        }

        data class Explicit(override val target: LeafToken, override val operator: LeafToken, override val predicates: List<PredicateToken>) : Assignment {
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