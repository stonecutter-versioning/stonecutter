package dev.kikugie.stitcher.antlr.layout

import dev.kikugie.commons.takeAs
import dev.kikugie.commons.takeAsOrNull
import dev.kikugie.commons.then
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.adapter.InlineCharStream
import dev.kikugie.stitcher.antlr.adapter.InlineTokenFactory
import dev.kikugie.stitcher.antlr.converter.DefinitionBuilder
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.DefinitionType
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.issue.ProblemCollector
import dev.kikugie.stitcher.issue.ProblemTemplate
import dev.kikugie.stitcher.util.LC
import dev.kikugie.stitcher.util.checkNot
import dev.kikugie.stitcher.util.skipLeadingSpaces
import dev.kikugie.stitcher.util.skipWhile
import dev.kikugie.stitcher.util.toLeaf
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.TerminalNode

private val SKIPPABLE_SPACES: CharArray = charArrayOf(' ', '\t', '\r', '\n')
private val LOST_ROOT_CLOSER: ProblemTemplate = ProblemTemplate("Unexpected scope closer")

private val ScopeBuilder.Code.isUnoccupied: Boolean
    get() = definition.type.isOpen && entries.isEmpty()

internal class LayoutBuilder(val stream: TokenStream, val problems: ProblemCollector) {
    private var builder: ScopeBuilder = ScopeBuilder.Root()
    private val current: Token get() = stream.LT(1)

    fun collect(): BlockToken.Root {
        while (stream.LA(1) != IntStream.EOF) when (current.type) {
            LayoutTokens.CONTENT -> handleContent(consume())
            LayoutTokens.COMMENT_OPEN -> handleComment(consume(), consume(), consume())
            else -> error("Unexpected token $current")
        }

        check(builder is ScopeBuilder.Root) { "The scope was not properly closed" }
        return builder.build() as BlockToken.Root
    }

    private fun consume(): Token = current.also { stream.consume() }
    private fun handleContent(token: Token): Unit = when (val it = builder) {
        is ScopeBuilder.Code if it.isUnoccupied -> appendContent(token)
        else -> it.content(token)
    }

    private fun appendContent(token: Token): Unit = InlineCharStream(token).invoke {
        if (!skipLeadingSpaces(*SKIPPABLE_SPACES))
            return@invoke builder.content(token)

        val unfinished = consumeScope(builder.takeAsOrNull<ScopeBuilder.Code>()?.definition?.type ?: INDEPENDENT)
        val range = if (unfinished) start..<host.index() else start..<end
        builder.content(range, host)
        builder = builder.parent!!

        if (unfinished) builder.content(host.index()..<end, host)
    }

    private fun handleComment(opener: Token, body: Token, closer: Token) {
        val (marker, definition) = parseComment(body)
            ?: return appendComment(opener, body, closer)
        appendDefinition(marker, definition)
    }

    private fun appendComment(opener: Token, body: Token, closer: Token) {
        builder.comment(opener, body, closer)
        if (builder is ScopeBuilder.Code && builder.takeAs<ScopeBuilder.Code>().definition.type.isOpen)
            builder = builder.parent!!
    }

    private fun appendDefinition(marker: LeafToken, definition: DefinitionToken): Unit = when (val it = builder) {
        is ScopeBuilder.Root -> appendRootDefinition(marker, definition)
        is ScopeBuilder.Code -> appendNestedDefinition(it, marker, definition)
        else -> error("Shouldn't be a builder")
    }

    private fun appendRootDefinition(marker: LeafToken, definition: DefinitionToken) {
        problems.checkNot(definition.type.isExtension, { LOST_ROOT_CLOSER.at(definition.closer!!.range.first) })
        val code = builder.code(marker, definition)
        if (!definition.type.isEmpty) builder = code
    }

    private fun appendNestedDefinition(host: ScopeBuilder.Code, marker: LeafToken, definition: DefinitionToken) {
        if (definition.type == INDEPENDENT) {
            builder.code(marker, definition)
            return
        }

        checkNot(definition.type.isExtension && host.marker.type != marker.type) { "Closes invalid scope" }
        checkNot(definition.type.isExtension && !host.definition.type.isScoped) { "Closes unscoped scope" }

        if (definition.type.isExtension) builder = builder.parent!!
        val code = builder.code(marker, definition)
        if (!definition.type.isEmpty) builder = code
    }

    private fun parseComment(body: Token): Pair<LeafToken, DefinitionToken>? {
        if (body.stopIndex < body.startIndex)
            return null // Empty comment body

        return InlineCharStream(body).invoke {
            when (LC(1)) {
                '?', '$', '~' -> {}
                else -> return@invoke null
            }

            val lexer = StitcherLexer(this).apply {
                tokenFactory = InlineTokenFactory(body)
            }
            val parser = StitcherParser(CommonTokenStream(lexer))
            val context = parser.definition()
            constructCode(context)
        }
    }

    private fun constructCode(def: StitcherParser.DefinitionContext): Pair<LeafToken, DefinitionToken> {
        var marker: TerminalNode
        var context: ParserRuleContext

        with(def) {
            COND_MARK()?.let {
                marker = it
                context = condition()
                return@with
            }

            SWAP_MARK()?.let {
                marker = it
                context = swap()
                return@with
            }

            REPL_MARK()?.let {
                marker = it
                context = replacement()
                return@with
            }

            error("Unreachable")
        }

        return marker.toLeaf() to context.accept(DefinitionBuilder)
    }

    private fun InlineCharStream.consumeScope(type: DefinitionType): Boolean = when (type) {
        LINE_OPENER, LINE_EXTENSION -> skipWhile {
            it != '\n' && it != '\r'
        }

        WORD_OPENER, WORD_EXTENSION -> skipWhile {
            it !in SKIPPABLE_SPACES
        }

        else -> seek(end) then false
    }
}