package dev.kikugie.stitcher.data

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.DefinitionType.*

private fun LeafToken?.openerType(): DefinitionType = when(this?.type) {
    StitcherParser.SCOPE_OPEN -> SCOPED_OPENER
    StitcherParser.SCOPE_WORD -> WORD_OPENER
    null -> LINE_OPENER
    else -> error("Invalid opener type $name")
}

private fun LeafToken?.extensionType(): DefinitionType = when(this?.type) {
    StitcherParser.SCOPE_OPEN -> SCOPED_EXTENSION
    StitcherParser.SCOPE_WORD -> WORD_EXTENSION
    null -> LINE_EXTENSION
    else -> error("Invalid opener type $name")
}

internal sealed interface DefinitionToken {
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
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitReplacement(this)
    }

    sealed interface Swap : DefinitionToken {
        val identifier: LeafToken? get() = null
        val arguments: List<LeafToken> get() = emptyList()

        data class Opener(override val identifier: LeafToken, override val arguments: List<LeafToken>, override val opener: LeafToken?) : Swap {
            override val type: DefinitionType get() = opener.openerType()
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitSwap(this)
        }

        data class Closer(override val closer: LeafToken) : Swap {
            override val type: DefinitionType get() = CLOSER
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
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCondition(this)
        }

        data class Extension(override val closer: LeafToken, override val sugar: List<LeafToken>,
                             override val expression: ExpressionToken?, override val opener: LeafToken?) : Condition {
            override val type: DefinitionType get() = opener.extensionType()
            override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCondition(this)
        }

        data class Closer(override val closer: LeafToken): Condition {
            override val type: DefinitionType get() = CLOSER
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