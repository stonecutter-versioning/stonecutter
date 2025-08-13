package dev.kikugie.stitcher.antlr.converter

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.StringVersion
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken.Assignment.Predicate
import dev.kikugie.stitcher.util.range

object PredicateBuilder : StitcherBaseVisitor<Predicate>() {
    override fun visitVersionPredicate(ctx: StitcherParser.VersionPredicateContext): Predicate =
        (ctx.semanticPredicate() ?: ctx.stringPredicate()).accept(this)

    override fun visitSemanticPredicate(ctx: StitcherParser.SemanticPredicateContext): Predicate {
        val comparator = ctx.semanticComparator()?.resolve()
            ?: ctx.stringComparator().resolve()
        // TODO: cache parsed result and better capture the error
        val node = ctx.LOOSE_VERSION()
        val version = SemanticVersion.parse(node.text)
            .getOrThrow()
        return Predicate(comparator, version, ctx.range, node.symbol.inputStream)
    }

    override fun visitStringPredicate(ctx: StitcherParser.StringPredicateContext): Predicate {
        val comparator = ctx.stringComparator().resolve()
        val node = ctx.IDENTIFIER()
        val version = StringVersion(node.text)
        return Predicate(comparator, version, ctx.range, node.symbol.inputStream)
    }

    private fun StitcherParser.SemanticComparatorContext.resolve(): VersionOperator? = when {
        COMP_MAJOR() != null -> SAME_MAJOR
        REPL_MARK() != null -> SAME_MINOR
        else -> null
    }

    private fun StitcherParser.StringComparatorContext.resolve(): VersionOperator = when {
        // TODO: Add OP_NOT matcher when it's implemented in VersionOperator
        COMP_MORE() != null -> if (COMP_EQUAL() != null) GREATER_EQUAL else GREATER
        COMP_LESS() != null -> if (COMP_EQUAL() != null) LESS_EQUAL else LESS
        COMP_EQUAL() != null -> EQUAL
        else -> error("$this is empty")
    }
}