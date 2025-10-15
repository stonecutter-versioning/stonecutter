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
import dev.kikugie.stitcher.antlr.InlineCharStream
import dev.kikugie.stitcher.antlr.InlineErrorListener
import dev.kikugie.stitcher.antlr.InlineTokenConverter
import dev.kikugie.stitcher.antlr.InlineTokenStream
import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode

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

private fun ScopeBuilder.code(opener: AntlrToken, body: AntlrToken, closer: AntlrToken, marker: LeafToken, definition: DefinitionToken): ScopeBuilder.Code =
    ScopeBuilder.Code(this, ScopeBuilder.Comment(this, opener, body, closer), marker, definition).also { entries as MutableList += it }

/**Returns `true` if there are characters left.*/
private fun InlineCharStream.consumeScope(type: DefinitionType): Boolean = when (type) {
    // The first line without the line break
    LINE_OPENER, LINE_EXTENSION -> skipNotMatching(*LINE_BREAKS)

    // The first "word" until a whitespace or a line break
    WORD_OPENER, WORD_EXTENSION -> skipNotMatching(*WHITESPACES)

    // Technically not needed - consumes the entire sequence
    else -> seek(end - 1) then false
}

private sealed interface ScopeBuilder {
    val parent: ScopeBuilder?
    val entries: List<ScopeBuilder>

    fun build(converter: InlineTokenConverter): BlockToken

    class Root(
        override val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ScopeBuilder {
        override val parent: ScopeBuilder? get() = null
        override fun build(converter: InlineTokenConverter): BlockToken.Root =
            BlockToken.Root(entries.map { it.build(converter) })
    }

    class Code(
        override val parent: ScopeBuilder,
        val host: Comment,
        val marker: LeafToken,
        val definition: DefinitionToken,
        override val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ScopeBuilder {
        override fun build(converter: InlineTokenConverter): BlockToken.Code =
            BlockToken.Code(host.build(converter), marker, definition, entries.map { it.build(converter) })
    }

    class Content(override val parent: ScopeBuilder, val tokens: MutableList<AntlrToken> = mutableListOf()) : ScopeBuilder {
        constructor(parent: ScopeBuilder, token: AntlrToken) : this (parent, mutableListOf(token))
        override val entries: List<ScopeBuilder> get() = emptyList()
        override fun build(converter: InlineTokenConverter): BlockToken.Content =
            BlockToken.Content(converter(tokens))
    }

    class Comment(override val parent: ScopeBuilder, val opener: AntlrToken, val body: AntlrToken, val closer: AntlrToken) : ScopeBuilder {
        override val entries: List<ScopeBuilder> get() = emptyList()
        override fun build(converter: InlineTokenConverter): BlockToken.Comment =
            BlockToken.Comment(converter(opener), converter(body), converter(closer))
    }
}

internal class LayoutBuilder private constructor(val stream: TokenStream, val sink: ProblemSink, val converter: InlineTokenConverter) {
    private val factory = CommonTokenFactory()
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
            val closer = builder.takeAs<ScopeBuilder.Code>().definition.opener
                ?.takeIf { it.type == StitcherParser.SCOPE_OPEN }
            if (closer != null)
                sink.at(closer) report problem { "Unclosed scope" }
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
            if (!skipMatching(*WHITESPACES))
                return@invoke builder.content(token)

            val unfinished = consumeScope(type)
            builder.content(factory.create(stream.tokenSource, CONTENT, start, host.index() - 1, 1, 0, host))
            builder = builder.parent!!

            // FIXME: Line and column are not counted in InlineCharStream
            if (unfinished) builder.content(factory.create(stream.tokenSource, CONTENT, host.index(), end - 1, 1, 0, host))
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
            builder.code(opener, body, closer, marker, definition).also { if (!definition.type.isEmpty) builder = it }
            return
        }

        if (builder is ScopeBuilder.Code) {
            val code = builder as ScopeBuilder.Code
            if (definition.type == INDEPENDENT) {
                builder.code(opener, body, closer, marker, definition)
                return
            }

            if (definition.type.isExtension && code.marker.type != marker.type)
                // TODO: Currently just opens a new scope like nothing, but should it?
                sink.at(marker) report problem { "Extension closes invalid scope ${code.marker.name.substringBefore('_')}" }

            if (definition.type.isExtension && code.definition.type.isOpen)
                sink.at(marker) report problem { "Extension closes an open scope" }

            if (definition.type.isExtension) builder = builder.parent!!
            builder.code(opener, body, closer, marker, definition).also { if (!definition.type.isEmpty) builder = it }
        }
    }
    private fun parseComment(body: Token): Pair<LeafToken, DefinitionToken>? {
        if (body.stopIndex < body.startIndex)
            return null // Empty comment body

        val input = body.text.toStream()
        when (input.LC(1)) {
            '?', '$', '~' -> {}
            else -> return null
        }

        val listener = InlineErrorListener(sink, FileLineIndex(input), body.startIndex)
        val lexer = StitcherLexer(input).errorListener(listener)
        val parser = StitcherParser(InlineTokenStream(lexer, body.startIndex)).errorListener(listener)
        val context = parser.definition()
        return constructCode(context)
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

        @JvmField val TOKEN_NAMES = listOf("CONTENT", "COMMENT_OPEN", "COMMENT_BODY", "COMMENT_CLOSE")
        @JvmField val VOCABULARY: Vocabulary = VocabularyImpl(emptyArray(), arrayOf(null, *TOKEN_NAMES.toTypedArray()))

        fun build(input: TokenStream, sink: ProblemSink, converter: InlineTokenConverter): BlockToken.Root =
            LayoutBuilder(input, sink, converter).collect()
    }
}