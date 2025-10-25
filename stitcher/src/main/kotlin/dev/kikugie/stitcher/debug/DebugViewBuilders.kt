package dev.kikugie.stitcher.debug

import dev.kikugie.commons.collections.present
import dev.kikugie.stitcher.data.composite.AssignmentExpression
import dev.kikugie.stitcher.data.composite.BinaryExpression
import dev.kikugie.stitcher.data.composite.BlockToken
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.CommentBlock
import dev.kikugie.stitcher.data.composite.ConditionDefinition
import dev.kikugie.stitcher.data.composite.ConstantExpression
import dev.kikugie.stitcher.data.composite.ContentBlock
import dev.kikugie.stitcher.data.composite.DefinitionToken
import dev.kikugie.stitcher.data.composite.ExpressionToken
import dev.kikugie.stitcher.data.composite.GroupExpression
import dev.kikugie.stitcher.data.composite.ReplacementDefinition
import dev.kikugie.stitcher.data.composite.RootBlock
import dev.kikugie.stitcher.data.composite.SwapDefinition
import dev.kikugie.stitcher.data.composite.UnaryExpression
import dev.kikugie.stitcher.data.custom.PredicateToken

internal fun BlockToken.debugView() = accept(BlockDebugViewBuilder)
internal fun DefinitionToken.debugView() = accept(DefinitionDebugViewBuilder)
internal fun ExpressionToken.debugView() = accept(ExpressionDebugViewBuilder)

internal fun PredicateToken.debugView() = debugView("PredicateToken") {
    properties = buildMap {
        this["predicate"] = text
    }
}

private object BlockDebugViewBuilder : BlockToken.Visitor<DebugView> {
    override fun visitContent(content: ContentBlock): DebugView = debugView("ContentBlock") {
        properties = buildMap {
            this["content"] = content.leaf.text.remap(Char::sanitizeMultiLine)
        }
    }

    override fun visitComment(comment: CommentBlock): DebugView = debugView("CommentBlock") {
        properties = buildMap {
            this["opener"] = comment.opener.text
            this["body"] = comment.body.text.remap(Char::sanitizeMultiLine)
            this["closer"] = comment.closer.text.remap(Char::sanitizeSingleLine)
        }
    }

    override fun visitCode(code: CodeBlock): DebugView = debugView("CodeBlock") {
        properties = buildMap {
            this["type"] = code.definition.type.toString()
            this["marker"] = code.marker.text
        }
        attributes = buildMap {
            this["definition"] = code.definition.accept(DefinitionDebugViewBuilder)
            this["comment"] = code.host.accept(BlockDebugViewBuilder)
        }
        items = code.scope.map { it.accept(BlockDebugViewBuilder) }
    }

    override fun visitRoot(root: RootBlock): DebugView = debugView("RootScope") {
        items = root.scope.map { it.accept(BlockDebugViewBuilder) }
    }
}

private object DefinitionDebugViewBuilder : DefinitionToken.Visitor<DebugView> {
    override fun visitReplacement(repl: ReplacementDefinition): DebugView = debugView("ReplacementDefinition") {
        properties = buildMap {
            this["identifier"] = repl.identifier.text
        }
    }

    override fun visitSwap(swap: SwapDefinition): DebugView = when (swap) {
        is SwapDefinition.Closer -> debugView("CloserSwapDefinition") {
            properties = buildMap {
                this["closer"] = swap.closer.text
            }
        }
        is SwapDefinition.Opener -> debugView("OpenerSwapDefinition") {
            properties = buildMap {
                this["identifier"] = swap.identifier.text
                if (swap.arguments.isNotEmpty())
                    this["arguments"] = swap.arguments.map { it.text }.present()
                swap.opener?.let { this["opener"] = it.text }
            }
        }
    }

    override fun visitCondition(cond: ConditionDefinition): DebugView = when(cond) {
        is ConditionDefinition.Closer -> debugView("CloserConditionDefinition") {
            properties = buildMap {
                this["closer"] = cond.closer.text
            }
        }
        is ConditionDefinition.Extension -> debugView("ExtensionConditionDefinition") {
            properties = buildMap {
                this["closer"] = cond.closer.text
                if (cond.sugar.isNotEmpty())
                    this["sugar"] = cond.sugar.map { it.text }.present()
                cond.opener?.let { this["opener"] = it.text }
            }
            attributes = buildMap {
                if (cond.expression != null)
                    this["expression"] = cond.expression.accept(ExpressionDebugViewBuilder)
            }
        }
        is ConditionDefinition.Opener -> debugView("OpenerConditionDefinition") {
            properties = buildMap {
                if (cond.sugar.isNotEmpty())
                    this["sugar"] = cond.sugar.map { it.text }.present()
                cond.opener?.let { this["opener"] = it.text }
            }
            attributes = buildMap {
                this["expression"] = cond.expression.accept(ExpressionDebugViewBuilder)
            }
        }
    }

}

private object ExpressionDebugViewBuilder : ExpressionToken.Visitor<DebugView> {
    override fun visitGroup(group: GroupExpression): DebugView = debugView("GroupExpression") {
        properties = buildMap {
            this["lb"] = group.lb.text
            this["rb"] = group.rb.text
        }
        attributes = buildMap {
            this["body"] = group.body.accept(ExpressionDebugViewBuilder)
        }
    }

    override fun visitUnary(unary: UnaryExpression): DebugView = debugView("UnaryOperator") {
        properties = buildMap {
            this["operator"] = unary.operator.text
        }
        attributes = buildMap {
            this["target"] = unary.target.accept(ExpressionDebugViewBuilder)
        }
    }

    override fun visitBinary(binary: BinaryExpression): DebugView = debugView("BinaryExpression") {
        properties = buildMap {
            this["operator"] = binary.operator.text
        }
        attributes = buildMap {
            this["left"] = binary.left.accept(ExpressionDebugViewBuilder)
            this["right"] = binary.right.accept(ExpressionDebugViewBuilder)
        }
    }

    override fun visitConstant(constant: ConstantExpression): DebugView = debugView("ConstantExpression") {
        properties = buildMap {
            this["value"] = constant.value.text
        }
    }

    override fun visitAssignment(assignment: AssignmentExpression): DebugView = debugView("AssignmentExpression") {
        properties = buildMap {
            assignment.target?.text?.let { this["target"] = it }
            assignment.operator?.text?.let { this["operator"] = it }
        }
        items = assignment.predicates.map(PredicateToken::debugView)
    }

}

private fun Char.sanitizeSingleLine() = when(this) {
    '\n' -> "\\n"
    ' ', '\t' -> "⋅"
    else -> toString()
}

private fun Char.sanitizeMultiLine() = when(this) {
    '\n' -> "\\n\n"
    ' ', '\t' -> "⋅"
    else -> toString()
}

private inline fun String.remap(action: (Char) -> String) = buildString {
    if (this@remap.isEmpty()) append("∅")
    else for (it in this@remap) append(action(it))
}