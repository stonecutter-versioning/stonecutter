package dev.kikugie.stitcher.parser.component

import dev.kikugie.commons.takeAsOrNull
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.StringVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.semver.data.VersionPredicate
import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.custom.PredicateToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.util.range
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode

private val ParseTree?.type: Int
    get() = this?.takeAsOrNull<TerminalNode>()?.symbol?.type ?: -1

internal class PredicateBuilder(sink: ProblemSink, val factory: StitcherTokenFactory) : StitcherBaseVisitor<PredicateToken>(), ProblemSource by sink {
    override fun visitSemanticPredicate(ctx: StitcherParser.SemanticPredicateContext): PredicateToken {
        val comparator = ctx.semanticComparator()?.resolve()
            ?: ctx.stringComparator()?.resolve()
            ?: VersionOperator.IMPLICIT_EQUAL
        val version = ctx.semanticVersion().build()
        return PredicateToken(VersionPredicate(comparator, version), ctx.range)
    }

    override fun visitStringPredicate(ctx: StitcherParser.StringPredicateContext): PredicateToken {
        val comparator = ctx.stringComparator().resolve()
        val version = ctx.stringVersion().build()
        return PredicateToken(VersionPredicate(comparator, version), ctx.range)
    }

    private fun ParserRuleContext.resolve(): VersionOperator = when(getChild(0).type) {
        StitcherLexer.COMP_EQUAL -> VersionOperator.EQUAL
        StitcherLexer.COMP_NEQUAL -> VersionOperator.NOT_EQUAL
        StitcherLexer.COMP_MORE -> VersionOperator.GREATER
        StitcherLexer.COMP_GMORE -> VersionOperator.GREATER_EQUAL
        StitcherLexer.COMP_LESS -> VersionOperator.LESS
        StitcherLexer.COMP_GLESS -> VersionOperator.LESS_EQUAL
        StitcherLexer.REPL_MARK -> VersionOperator.SAME_MINOR
        StitcherLexer.COMP_MAJOR -> VersionOperator.SAME_MAJOR
        else -> at(start) bail "Unable to resolve version operator"
    }

    private fun StitcherParser.StringVersionContext.build(): Version =
        StringVersion(text)

    private fun StitcherParser.SemanticVersionContext.build(): Version {
        val components = versionCore().NUMERIC().map<TerminalNode, Int> { it.text.toInt() }.toIntArray()
        val preRelease = preRelease()?.text.orEmpty()
        val buildMetadata = buildMetadata()?.text.orEmpty()
        return SemanticVersion(components, preRelease, buildMetadata)
    }
}