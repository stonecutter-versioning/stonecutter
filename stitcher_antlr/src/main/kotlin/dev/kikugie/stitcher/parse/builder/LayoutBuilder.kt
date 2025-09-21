package dev.kikugie.stitcher.parse.builder

import dev.kikugie.commons.takeAs
import dev.kikugie.commons.takeAsOrNull
import dev.kikugie.commons.then
import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.DefinitionType
import dev.kikugie.stitcher.data.DefinitionType.*
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.parse.adapter.AntlrTokenConverter
import dev.kikugie.stitcher.parse.adapter.InlineCharStream
import dev.kikugie.stitcher.parse.adapter.InlineTokenFactory
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode

private val SKIPPABLE_SPACES: CharArray = charArrayOf(' ', '\t', '\r', '\n')

private fun ScopeBuilder.isPopulated(): Boolean = entries.isNotEmpty() && !entries.first().let { builder ->
    builder is ScopeBuilder.Content && builder.tokens.all { it.text.isBlank() }
}

private fun ScopeBuilder.content(token: AntlrToken): Unit = when (val last = entries.lastOrNull()) {
    is ScopeBuilder.Content -> last.tokens += token
    else -> entries as MutableList += ScopeBuilder.Content(this, token)
}

private fun ScopeBuilder.comment(opener: AntlrToken, body: AntlrToken, closer: AntlrToken) {
    entries as MutableList += ScopeBuilder.Comment(this, opener, body, closer)
}

private fun ScopeBuilder.code(opener: AntlrToken, marker: LeafToken, definition: DefinitionToken, closer: AntlrToken): ScopeBuilder.Code =
    ScopeBuilder.Code(this, opener, marker, definition, closer).also { entries as MutableList += it }

/**Returns `true` if there are characters left.*/
private fun InlineCharStream.consumeScope(type: DefinitionType): Boolean = when (type) {
    // The first line without the line break
    LINE_OPENER, LINE_EXTENSION -> skipWhile {
        it != '\n' && it != '\r'
    }

    // The first "word" until a whitespace or a line break
    WORD_OPENER, WORD_EXTENSION -> skipWhile {
        it !in SKIPPABLE_SPACES
    }

    // Technically not needed - consumes the entire sequence
    else -> seek(end) then false
}

private sealed interface ScopeBuilder {
    val parent: ScopeBuilder?
    val entries: List<ScopeBuilder>

    fun build(converter: AntlrTokenConverter): BlockToken

    class Root(
        override val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ScopeBuilder {
        override val parent: ScopeBuilder? get() = null
        override fun build(converter: AntlrTokenConverter): BlockToken.Root =
            BlockToken.Root(entries.map { it.build(converter) })
    }

    class Code(
        override val parent: ScopeBuilder,
        val opener: AntlrToken,
        val marker: LeafToken,
        val definition: DefinitionToken,
        val closer: AntlrToken,
        override val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ScopeBuilder {
        override fun build(converter: AntlrTokenConverter): BlockToken.Code =
            BlockToken.Code(converter(opener), marker, definition, converter(closer), entries.map { it.build(converter) })
    }

    class Content(override val parent: ScopeBuilder, val tokens: MutableList<AntlrToken> = mutableListOf()) : ScopeBuilder {
        constructor(parent: ScopeBuilder, token: AntlrToken) : this (parent, mutableListOf(token))
        override val entries: List<ScopeBuilder> get() = emptyList()
        override fun build(converter: AntlrTokenConverter): BlockToken.Content =
            BlockToken.Content(converter(*tokens.toTypedArray()))
    }

    class Comment(override val parent: ScopeBuilder, val opener: AntlrToken, val body: AntlrToken, val closer: AntlrToken) : ScopeBuilder {
        override val entries: List<ScopeBuilder> get() = emptyList()
        override fun build(converter: AntlrTokenConverter): BlockToken.Comment =
            BlockToken.Comment(converter(opener), converter(body), converter(closer))
    }
}

internal class LayoutBuilder private constructor(val stream: TokenStream, val sink: ProblemSink, val converter: AntlrTokenConverter) {
    private val visitor = DefinitionBuilder(sink, converter)
    private var builder: ScopeBuilder = ScopeBuilder.Root()
    private val current: AntlrToken get() = stream.LT(1)

    private fun collect(): BlockToken.Root {
        while (stream.LA(1) != IntStream.EOF) when (current.type) {
            CONTENT -> handleContent(consume())
            COMMENT_OPEN -> handleComment(consume(), consume(), consume())
            else -> error("Unexpected token $current")
        }

        while (builder !is ScopeBuilder.Root) {
            // TODO: Report at the comment
            sink.at(current) report problem { "Unclosed scope" }
            builder = builder.parent!!
        }

        return builder.takeAs<ScopeBuilder.Root>().build(converter)
    }

    private fun consume(): AntlrToken = current.also { stream.consume() }

    private fun handleContent(token: AntlrToken) {
        // Root scope doesn't need to split content
        if (builder is ScopeBuilder.Root)
            return builder.content(token)

        // When inside closed brackets no need to split either
        val type = builder.takeAs<ScopeBuilder.Code>().definition.type.apply {
            if (isScoped) return builder.content(token)
        }

        InlineCharStream(token).invoke {
            // The entire block is whitespace - append and wait for the next
            if (!skipLeadingSpaces(*SKIPPABLE_SPACES))
                return@invoke builder.content(token)

            val unfinished = consumeScope(type)
            val factory = InlineTokenFactory(token)
            builder.content(factory.create(stream.tokenSource, CONTENT, start, host.index() - 1, 1, 0))
            builder = builder.parent!!

            // FIXME: Line and offset are not counted in InlineCharStream
            if (unfinished) builder.content(factory.create(stream.tokenSource, CONTENT, host.index(), end - 1, 1, 0))
        }
    }

    private fun handleComment(opener: Token, body: Token, closer: Token) {
        val (marker, definition) = parseComment(body) ?: kotlin.run {
            builder.comment(opener, body, closer)
            if (builder.takeAsOrNull<ScopeBuilder.Code>()?.definition?.type?.isOpen == true)
                builder = builder.parent!!
            return
        }

        if (builder is ScopeBuilder.Root) {
            if (definition.closer != null) sink.at(definition.closer!!) report problem { "Unmatched scope closer" }
            builder.code(opener, marker, definition, closer).also { if (!definition.type.isEmpty) builder = it }
            return
        }

        if (builder is ScopeBuilder.Code) {
            val code = builder as ScopeBuilder.Code
            if (definition.type == INDEPENDENT) {
                builder.code(opener, marker, definition, closer)
                return
            }

            if (definition.type.isExtension && code.marker.type != marker.type)
                // TODO: Currently just opens a new scope like nothing, but should it?
                sink.at(marker) report problem { "Extension closes invalid scope ${code.marker.name.substringBefore('_')}" }

            if (definition.type.isExtension && code.definition.type.isOpen)
                sink.at(marker) report problem { "Extension closes an open scope" }

            if (definition.type.isExtension) builder = builder.parent!!
            builder.code(opener, marker, definition, closer).also { if (!definition.type.isEmpty) builder = it }
        }
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

        return converter(marker) to context.accept(visitor)
    }

    companion object {
        const val CONTENT: Int = 1
        const val COMMENT_OPEN: Int = 2
        const val COMMENT_BODY: Int = 3
        const val COMMENT_CLOSE: Int = 4

        @JvmField val VOCABULARY: Vocabulary = VocabularyImpl(
            emptyArray(),
            arrayOf("CONTENT", "COMMENT_OPEN", "COMMENT_BODY", "COMMENT_CLOSE")
        )

        fun build(input: TokenStream, sink: ProblemSink, converter: AntlrTokenConverter): BlockToken.Root =
            LayoutBuilder(input, sink, converter).collect()
    }
}