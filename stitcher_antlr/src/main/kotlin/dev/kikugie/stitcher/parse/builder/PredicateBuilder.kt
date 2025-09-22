@file:Suppress("UNCHECKED_CAST")

package dev.kikugie.stitcher.parse.builder

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.StringVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.semver.impl.VersionParsingException
import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.PredicateToken
import dev.kikugie.stitcher.issue.BailException
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.bail
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.parse.inline.InlineTokenConverter
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.concurrent.ConcurrentHashMap

internal class PredicateBuilder(val sink: ProblemSink, val converter: InlineTokenConverter) : StitcherBaseVisitor<PredicateToken>() {
    override fun visitVersionPredicate(ctx: StitcherParser.VersionPredicateContext): PredicateToken =
        (ctx.semanticPredicate() ?: ctx.stringPredicate()).accept(this)

    override fun visitSemanticPredicate(ctx: StitcherParser.SemanticPredicateContext): PredicateToken {
        val comparator = ctx.semanticComparator()?.resolve()
            ?: ctx.stringComparator().resolve()
        val node = ctx.LOOSE_VERSION()
        val version = cacheVersion(SEMVER_CACHE, SemanticVersion, node)
        return PredicateToken(comparator, version, converter(StitcherParser.LOOSE_VERSION, ctx))
    }

    override fun visitStringPredicate(ctx: StitcherParser.StringPredicateContext): PredicateToken {
        val comparator = ctx.stringComparator().resolve()
        val node = ctx.IDENTIFIER()
        val version = cacheVersion(STRVER_CACHE, StringVersion, node)
        return PredicateToken(comparator, version, converter(StitcherParser.LOOSE_VERSION, ctx))
    }

    private fun StitcherParser.SemanticComparatorContext.resolve(): VersionOperator? = when {
        COMP_MAJOR() != null -> SAME_MAJOR
        REPL_MARK() != null -> SAME_MINOR
        else -> null
    }

    private fun StitcherParser.StringComparatorContext.resolve(): VersionOperator = when {
        OP_NOT() != null -> NOT_EQUAL
        COMP_MORE() != null -> if (COMP_EQUAL() != null) GREATER_EQUAL else GREATER
        COMP_LESS() != null -> if (COMP_EQUAL() != null) LESS_EQUAL else LESS
        COMP_EQUAL() != null -> EQUAL
        else -> error("$this is empty")
    }

    private fun cacheVersion(cache: MutableMap<String, Version>, parser: Version.Operations, node: TerminalNode): Version = try {
        cache.computeIfAbsent(node.text) { parseVersion(parser, it, node.symbol) }
    } catch (_: BailException) {
        StringVersion("%PLACEHOLDER% (${node.text})")
    }

    private fun parseVersion(parser: Version.Operations, input: String, token: Token): Version = try {
        parser.parse(input).getOrThrow()
    } catch (e: VersionParsingException) {
        sink.at(token) bail problem(e) { "Failed to parse version '$input'" }
    }

    companion object {
        private val SEMVER_CACHE: MutableMap<String, Version> = ConcurrentHashMap()
        private val STRVER_CACHE: MutableMap<String, Version> = ConcurrentHashMap()
    }
}