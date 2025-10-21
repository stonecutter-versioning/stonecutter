package dev.kikugie.stitcher.data.composite

import dev.kikugie.stitcher.data.custom.ClosedScope
import dev.kikugie.stitcher.data.custom.ScopeToken
import dev.kikugie.stitcher.data.custom.WordScope
import dev.kikugie.stitcher.data.leaf.LeafToken

private fun ScopeToken?.openerType(): DefinitionToken.Type = when (this) {
    is ClosedScope -> DefinitionToken.Type.SCOPED_OPENER
    is WordScope -> DefinitionToken.Type.WORD_OPENER
    null -> DefinitionToken.Type.LINE_OPENER
}

private fun ScopeToken?.extensionType(): DefinitionToken.Type = when (this) {
    is ClosedScope -> DefinitionToken.Type.SCOPED_EXTENSION
    is WordScope -> DefinitionToken.Type.WORD_EXTENSION
    null -> DefinitionToken.Type.LINE_EXTENSION
}

internal sealed interface DefinitionToken {
    val closer: LeafToken? get() = null
    val opener: ScopeToken? get() = null

    val type: Type

    fun <T> accept(visitor: Visitor<T>): T

    interface Visitor<T> {
        fun visitSwap(swap: SwapDefinition): T
        fun visitReplacement(repl: ReplacementDefinition): T
        fun visitCondition(cond: ConditionDefinition): T
    }

    enum class Type {
        SCOPED_OPENER, LINE_OPENER, WORD_OPENER,
        SCOPED_EXTENSION, LINE_EXTENSION, WORD_EXTENSION,
        CLOSER, INDEPENDENT;

        val isScoped: Boolean
            get() = when (this) {
                SCOPED_OPENER, SCOPED_EXTENSION -> true
                else -> false
            }

        val isOpen: Boolean
            get() = when (this) {
                LINE_OPENER, WORD_OPENER, LINE_EXTENSION, WORD_EXTENSION -> true
                else -> false
            }

        val isExtension: Boolean
            get() = when (this) {
                SCOPED_EXTENSION, LINE_EXTENSION, WORD_EXTENSION, CLOSER -> true
                else -> false
            }

        val isEmpty: Boolean
            get() = when (this) {
                CLOSER, INDEPENDENT -> true
                else -> false
            }
    }
}

internal data class ReplacementDefinition(val identifier: LeafToken) : DefinitionToken {
    override val type: DefinitionToken.Type get() = DefinitionToken.Type.INDEPENDENT
    override fun <T> accept(visitor: DefinitionToken.Visitor<T>): T = visitor.visitReplacement(this)
}

internal sealed interface SwapDefinition : DefinitionToken {
    val identifier: LeafToken? get() = null
    val arguments: List<LeafToken> get() = emptyList()

    data class Opener(override val identifier: LeafToken, override val arguments: List<LeafToken>, override val opener: ScopeToken?) : SwapDefinition {
        override val type: DefinitionToken.Type get() = opener.openerType()
        override fun <T> accept(visitor: DefinitionToken.Visitor<T>): T = visitor.visitSwap(this)
    }

    data class Closer(override val closer: LeafToken) : SwapDefinition {
        override val type: DefinitionToken.Type get() = DefinitionToken.Type.CLOSER
        override fun <T> accept(visitor: DefinitionToken.Visitor<T>): T = visitor.visitSwap(this)
    }
}

internal sealed interface ConditionDefinition : DefinitionToken {
    val sugar: List<LeafToken> get() = emptyList()
    val expression: ExpressionToken? get() = null

    data class Opener(override val sugar: List<LeafToken>, override val expression: ExpressionToken, override val opener: ScopeToken?) : ConditionDefinition {
        override val type: DefinitionToken.Type get() = opener.openerType()
        override fun <T> accept(visitor: DefinitionToken.Visitor<T>): T = visitor.visitCondition(this)
    }

    data class Extension(
        override val closer: LeafToken, override val sugar: List<LeafToken>,
        override val expression: ExpressionToken?, override val opener: ScopeToken?
    ) : ConditionDefinition {
        override val type: DefinitionToken.Type get() = opener.extensionType()
        override fun <T> accept(visitor: DefinitionToken.Visitor<T>): T = visitor.visitCondition(this)
    }

    data class Closer(override val closer: LeafToken) : ConditionDefinition {
        override val type: DefinitionToken.Type get() = DefinitionToken.Type.CLOSER
        override fun <T> accept(visitor: DefinitionToken.Visitor<T>): T = visitor.visitCondition(this)
    }
}