package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.util.merge
import org.antlr.v4.runtime.CharStream

private fun LeafToken?.openerType(): DefinitionType = when(this?.type) {
    StitcherParser.SCOPE_OPEN -> SCOPED_OPENER
    StitcherParser.SCOPE_WORD -> WORD_OPENER
    null -> LINE_OPENER
    else -> error("Invalid opener type $typeName")
}

private fun LeafToken?.extensionType(): DefinitionType = when(this?.type) {
    StitcherParser.SCOPE_OPEN -> SCOPED_EXTENSION
    StitcherParser.SCOPE_WORD -> WORD_EXTENSION
    null -> LINE_EXTENSION
    else -> error("Invalid opener type $typeName")
}

internal sealed interface DefinitionToken : StitcherToken {
    val closer: LeafToken? get() = null
    val opener: LeafToken? get() = null

    val type: DefinitionType
    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitSwap(it: Swap): T
        fun visitReplacement(it: Replacement): T
        fun visitCondition(it: Condition): T
    }

    data class Replacement(val identifier: LeafToken) : DefinitionToken {
        override val type: DefinitionType get() = INDEPENDENT
        override val range: IntRange get() = identifier.range
        override val source: CharStream get() = identifier.source
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitReplacement(this)
    }

    sealed interface Swap : DefinitionToken {
        val identifier: LeafToken? get() = null
        val arguments: List<LeafToken> get() = emptyList()

        data class Opener(override val identifier: LeafToken, override val arguments: List<LeafToken>, override val opener: LeafToken?) : Swap {
            override val type: DefinitionType get() = opener.openerType()
            override val range: IntRange get() = merge(identifier.range, arguments.lastOrNull()?.range, opener?.range)
            override val source: CharStream get() = identifier.source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitSwap(this)
        }

        data class Closer(override val closer: LeafToken) : Swap {
            override val type: DefinitionType get() = CLOSER
            override val range: IntRange get() = closer.range
            override val source: CharStream get() = closer.source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitSwap(this)
        }

        companion object {
            operator fun invoke(identifier: LeafToken, arguments: List<LeafToken>, opener: LeafToken?): Swap =
                Opener(identifier, arguments, opener)
            operator fun invoke(closer: LeafToken): Swap =
                Closer(closer)
        }
    }

    sealed interface Condition : DefinitionToken {
        val sugar: List<LeafToken> get() = emptyList()
        val expression: ExpressionToken? get() = null

        data class Opener(override val sugar: List<LeafToken>, override val expression: ExpressionToken, override val opener: LeafToken?) : Condition {
            override val type: DefinitionType get() = opener.openerType()
            override val range: IntRange get() = merge(sugar.firstOrNull()?.range, expression.range, opener?.range)
            override val source: CharStream get() = expression.source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCondition(this)
        }

        data class Extension(override val closer: LeafToken, override val sugar: List<LeafToken>,
                             override val expression: ExpressionToken?, override val opener: LeafToken?) : Condition {
            override val type: DefinitionType get() = opener.extensionType()
            override val range: IntRange get() = merge(closer.range, sugar.lastOrNull()?.range, expression?.range, opener?.range)
            override val source: CharStream get() = closer.source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCondition(this)
        }

        data class Closer(override val closer: LeafToken): Condition {
            override val type: DefinitionType get() = CLOSER
            override val range: IntRange get() = closer.range
            override val source: CharStream get() = closer.source
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCondition(this)
        }

        companion object {
            operator fun invoke(sugar: List<LeafToken>, expression: ExpressionToken, opener: LeafToken?): Condition =
                Opener(sugar, expression, opener)
            operator fun invoke(closer: LeafToken, sugar: List<LeafToken>, expression: ExpressionToken?, opener: LeafToken?): Condition =
                Extension(closer, sugar, expression, opener)
            operator fun invoke(closer: LeafToken): Condition =
                Closer(closer)
        }
    }
}