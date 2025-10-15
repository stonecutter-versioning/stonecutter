package dev.kikugie.stitcher.parser.component

import dev.kikugie.commons.takeAsOrNull
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.StringVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.data.PredicateToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.bail
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode

private val ParseTree?.type: Int
    get() = this?.takeAsOrNull<TerminalNode>()?.symbol?.type ?: -1

internal class PredicateBuilder(val problems: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<PredicateToken>() {
    override fun visitSemanticPredicate(ctx: StitcherParser.SemanticPredicateContext): PredicateToken {
        val comparator = ctx.semanticComparator()?.resolve()
            ?: ctx.stringComparator()?.resolve()
            ?: VersionOperator.IMPLICIT_EQUAL
        val version = ctx.semanticVersion().build()
        return PredicateToken(comparator, version, factory.fromAntlrRule(LeafToken.Type(StitcherLexer.NUMERIC), ctx))
    }

    override fun visitStringPredicate(ctx: StitcherParser.StringPredicateContext): PredicateToken {
        val comparator = ctx.stringComparator().resolve()
        val version = ctx.stringVersion().build()
        return PredicateToken(comparator, version, factory.fromAntlrRule(LeafToken.Type(StitcherLexer.IDENTIFIER), ctx))
    }

    private fun StitcherParser.StringComparatorContext.resolve(): VersionOperator = when(getChild(0).type) {
        StitcherLexer.COMP_EQUAL -> VersionOperator.EQUAL
        StitcherLexer.COMP_NEQUAL -> VersionOperator.NOT_EQUAL
        StitcherLexer.COMP_MORE -> VersionOperator.GREATER
        StitcherLexer.COMP_GMORE -> VersionOperator.GREATER_EQUAL
        StitcherLexer.COMP_LESS -> VersionOperator.LESS
        StitcherLexer.COMP_GLESS -> VersionOperator.LESS_EQUAL
        else -> problems.at(start) bail problem { "Unable to resolve version operator" }
    }

    private fun StitcherParser.SemanticComparatorContext.resolve(): VersionOperator = when(getChild(0).type) {
        StitcherLexer.REPL_MARK -> VersionOperator.SAME_MINOR
        StitcherLexer.COMP_MAJOR -> VersionOperator.SAME_MAJOR
        else -> problems.at(start) bail problem { "Unable to resolve version operator" }
    }

    private fun StitcherParser.StringVersionContext.build(): Version = StringVersion(text)

    private fun StitcherParser.SemanticVersionContext.build(): Version {
        val components = versionCore().NUMERIC().map<TerminalNode, Int> { it.text.toInt() }.toIntArray()
        val preRelease = preRelease()?.text.orEmpty()
        val buildMetadata = buildMetadata()?.text.orEmpty()
        return SemanticVersion(components, preRelease, buildMetadata)
    }
}