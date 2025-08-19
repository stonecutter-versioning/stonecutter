package dev.kikugie.stitcher.transformer

import dev.kikugie.stitcher.api.TransformParameters
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.util.get
import dev.kikugie.stitcher.util.merge
import dev.kikugie.stitcher.util.verify

internal data class TransformationBuilder(
    val parameters: TransformParameters,
    val changes: MutableList<SourceTransformation> = mutableListOf()
) : BlockToken.Visitor<Unit> {
    override fun visitContent(it: BlockToken.Content) = Unit
    override fun visitComment(it: BlockToken.Comment) = Unit
    override fun visitRoot(it: BlockToken.Root) {
        for (block in it.scope) block.accept(this)
    }

    override fun visitCode(it: BlockToken.Code) {
        it.definition.accept(DefinitionVisitor(it.scope))
    }

    private inner class DefinitionVisitor(val scope: List<BlockToken>) : DefinitionToken.Visitor<Unit> {
        override fun visitReplacement(it: DefinitionToken.Replacement) {
            TODO("Not yet implemented")
        }

        override fun visitSwap(it: DefinitionToken.Swap) {
            val identifier = it.identifier?.text ?: return // Skip closer swaps
            val replacement = verify(parameters.swaps[identifier]) { "Undefined swap '$identifier'" }

            val range = scope.run { merge(first().range, last().range) }
            val model = ScopeModel(it.source[range])
            val result = model.prefix + replacement.replaceIndent(model.indent) + model.postfix
            changes += ReplaceTransformation(range, result)
        }

        override fun visitCondition(it: DefinitionToken.Condition) {
            TODO("Not yet implemented")
        }
    }
}