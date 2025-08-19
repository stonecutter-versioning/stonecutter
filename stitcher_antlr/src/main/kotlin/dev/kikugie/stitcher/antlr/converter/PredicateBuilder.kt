package dev.kikugie.stitcher.antlr.converter

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.StringVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.semver.data.VersionOperator
import dev.kikugie.semver.impl.VersionParsingException
import dev.kikugie.stitcher.antlr.StitcherBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken.Assignment.Predicate
import dev.kikugie.stitcher.util.range
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.concurrent.ConcurrentHashMap

internal object PredicateBuilder : StitcherBaseVisitor<Predicate>() {
    private val SEMVER_CACHE: MutableMap<String, SemanticVersion> = ConcurrentHashMap()
    private val STRVER_CACHE: MutableMap<String, StringVersion> = ConcurrentHashMap()

    override fun visitVersionPredicate(ctx: StitcherParser.VersionPredicateContext): Predicate =
        (ctx.semanticPredicate() ?: ctx.stringPredicate()).accept(this)

    override fun visitSemanticPredicate(ctx: StitcherParser.SemanticPredicateContext): Predicate {
        val comparator = ctx.semanticComparator()?.resolve()
            ?: ctx.stringComparator().resolve()
        val node = ctx.LOOSE_VERSION()
        val version = cacheVersion(SEMVER_CACHE, SemanticVersion, node)
        return Predicate(comparator, version, ctx.range, node.symbol.inputStream)
    }

    override fun visitStringPredicate(ctx: StitcherParser.StringPredicateContext): Predicate {
        val comparator = ctx.stringComparator().resolve()
        val node = ctx.IDENTIFIER()
        val version = cacheVersion(STRVER_CACHE, StringVersion, node)
        return Predicate(comparator, version, ctx.range, node.symbol.inputStream)
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

    private fun <T : Version> cacheVersion(cache: MutableMap<String, T>, parser: Version.Operations, node: TerminalNode): T =
        cache.computeIfAbsent(node.text) { parseVersion(parser, it, node.symbol.startIndex) }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Version> parseVersion(parser: Version.Operations, input: String, offset: Int): T = parser.parse(input).getOrElse {
        // TODO: replace with an error reporting system
        it as VersionParsingException; throw VersionParsingException(it.message, it.position + offset).apply {
            stackTrace = it.stackTrace
        }
    } as T
}