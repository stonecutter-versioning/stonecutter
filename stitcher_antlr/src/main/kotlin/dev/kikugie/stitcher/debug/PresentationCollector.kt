package dev.kikugie.stitcher.debug

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.data.ExpressionToken.Assignment.Predicate
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.debug.TokenPresentation.Leaf
import dev.kikugie.stitcher.util.get
import org.antlr.v4.runtime.CharStream

private fun LeafToken.acceptThis() = PresentationCollector.visitLeaf(this)
private fun Predicate.acceptThis() = PresentationCollector.visitPredicate(this)
private fun composite(token: StitcherToken, builder: suspend SequenceScope<NamedPresentation>.() -> Unit) = TokenPresentation.Struct(token, sequence(builder))
private fun String.takeNEpsilon(n: Int, epsilon: String = "…"): String =
    if (this.length > n) this.take(n) + epsilon else this

private fun primitive(name: String, source: CharStream, range: IntRange) = object : TokenPresentation {
    override val type: String = name
    override val range: IntRange = range
    override val value: String get() = source[range].takeNEpsilon(50).replace("\n", "\\n")
}

internal object PresentationCollector :
    BlockToken.Visitor<TokenPresentation>,
    DefinitionToken.Visitor<TokenPresentation>,
    ExpressionToken.Visitor<TokenPresentation>
{
    override fun visitContent(it: BlockToken.Content) = primitive("Content", it.source, it.range)

    override fun visitComment(it: BlockToken.Comment) = composite(it) {
        yield("opener" to primitive("Opener", it.source, it.opener))
        yield("body" to it.body.accept(this@PresentationCollector))
        yield("closer" to primitive("Closer", it.source, it.closer))
    }
    override fun visitCode(it: BlockToken.Code) = composite(it) {
        yield("marker" to it.marker.acceptThis())
        yield("definition" to it.definition.accept(this@PresentationCollector))
        for (block in it.scope)
            yield("scope" to block.accept(this@PresentationCollector))
    }

    override fun visitRoot(it: BlockToken.Root) = composite(it) {
        for (block in it.scope)
            yield("scope" to block.accept(this@PresentationCollector))
    }

    override fun visitSwap(it: DefinitionToken.Swap) = composite(it) {
        it.closer?.run { yield("closer" to acceptThis()) }
        it.identifier?.run {yield("identifier" to acceptThis())  }
        it.opener?.run { yield("opener" to acceptThis()) }
    }

    override fun visitReplacement(it: DefinitionToken.Replacement) = composite(it) {
        yield("identifier" to it.identifier.acceptThis())
    }

    override fun visitCondition(it: DefinitionToken.Condition) = composite(it) {
        it.closer?.run { yield("closer" to acceptThis()) }
        for (entry in it.sugar)
            yield("sugar" to entry.acceptThis())
        it.expression?.run { yield("expression" to accept(this@PresentationCollector)) }
        it.opener?.run { yield("opener" to acceptThis()) }
    }

    override fun visitGroup(it: ExpressionToken.Group)= composite(it) {
        yield("left_brace" to it.lb.acceptThis())
        yield("body" to it.body.accept(this@PresentationCollector))
        yield("right_brace" to it.rb.acceptThis())
    }
    override fun visitUnary(it: ExpressionToken.Unary)= composite(it) {
        yield("operator" to it.operator.acceptThis())
        yield("operand" to it.operand.accept(this@PresentationCollector))
    }

    override fun visitBinary(it: ExpressionToken.Binary) = composite(it) {
        yield("left" to it.left.accept(this@PresentationCollector))
        yield("operator" to it.operator.acceptThis())
        yield("right" to it.right.accept(this@PresentationCollector))
    }

    override fun visitConstant(it: ExpressionToken.Constant) = composite(it) {
        yield("value" to it.value.acceptThis())
    }

    override fun visitAssignment(it: ExpressionToken.Assignment) = composite(it) {
        it.target?.run { yield("target" to acceptThis()) }
        it.operator?.run { yield("operator" to acceptThis()) }
        for (predicate in it.predicates)
            yield("predicates" to predicate.acceptThis())
    }

    fun visitLeaf(it: LeafToken): TokenPresentation = Leaf(it, StitcherParser.VOCABULARY)

    fun visitPredicate(it: Predicate): TokenPresentation = object : TokenPresentation {
        override val type: String = "Predicate"
        override val range: IntRange = it.range
        override val value: String = "${it.comparator.literal}${it.version}"
    }
}