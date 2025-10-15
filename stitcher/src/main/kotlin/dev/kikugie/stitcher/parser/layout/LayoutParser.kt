package dev.kikugie.stitcher.parser.layout

import dev.kikugie.commons.takeAs
import dev.kikugie.commons.then
import dev.kikugie.stitcher.antlr.*
import dev.kikugie.stitcher.antlr.InlineCharStream.Companion.inlineStream
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.DefinitionType
import dev.kikugie.stitcher.data.DefinitionType.*
import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.component.DefinitionBuilder
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.Token.EOF
import java.util.*

private fun InlineCharStream.consumeScope(type: DefinitionType): Boolean = when (type) {
    // The first line without the line break
    LINE_OPENER, LINE_EXTENSION -> skipNotMatching(*LINE_BREAKS)

    // The first "word" until a whitespace or a line break
    WORD_OPENER, WORD_EXTENSION -> skipNotMatching(*WHITESPACES)

    // Technically not needed - consumes the entire sequence
    else -> seek(end - 1) then false
}

private fun Int.scopeType(): String = when(this) {
    StitcherLexer.COND_MARK -> "condition"
    StitcherLexer.SWAP_MARK -> "swap"
    StitcherLexer.REPL_MARK -> "replacement"
    else -> "unknown"
}

internal class LayoutParser private constructor(stream: TokenStream, val problems: ProblemSink, val factory: StitcherTokenFactory) {
    private val tokens: TokenQueue = TokenQueue(stream)
    private val stack: Deque<ContainerScopeBuilder> = ArrayDeque<ContainerScopeBuilder>(4).apply { push(ScopeBuilder.Root()) }
    private val visitor: StitcherVisitor<Pair<AntlrToken, DefinitionToken>> = DefinitionBuilder.paired(problems, factory)

    private val builder: ContainerScopeBuilder get() = stack.peekLast()
    private val antlrSource: TokenSource get() = tokens.stream.tokenSource
    private val antlrFactory: TokenFactory<*> get() = antlrSource.tokenFactory


    private fun collect(): BlockToken.Root {
        while (tokens.hasMore()) when (tokens.current.type) {
            CONTENT -> handleContent(tokens.consume())
            COMMENT_OPEN -> handleComment(tokens.consume(), tokens.consume(), tokens.consume())
            EOF -> break
            else -> problems.at(tokens.consume()) report problem { "Unexpected token" }
        }

        while (stack.isNotEmpty()) when (val it = stack.removeLast()) {
            is ScopeBuilder.Root -> return it.build(factory)
            is ScopeBuilder.Code -> {
                val closer = it.definition.closer
                if (closer != null) problems.at(closer) report problem { "Unclosed scope" }
            }
        }
        error("Root scope was consumed")
    }

    private fun handleContent(token: AntlrToken) {
        // Root and closed scopes accept any content
        if (builder.acceptsAny()) builder += ScopeBuilder.Content(token)
        else handleSplitContent(token)
    }

    private fun handleSplitContent(token: AntlrToken): Unit = inlineStream(token) {
        // If the entire block is blank - add it and don't leave the scope
        if (!skipMatching(*WHITESPACES))
            return@inlineStream builder.plusAssign(ScopeBuilder.Content(token))

        // Add the respective scope and remove the builder from the stack
        val hasMoreCharacters = consumeScope(builder.takeAs<ScopeBuilder.Code>().type)
        val innerContentToken = antlrFactory.create(antlrSource, CONTENT, start, host.index() - 1, 1, 0, host)
        stack.removeLast() += ScopeBuilder.Content(innerContentToken)

        // If there's still content - add it to the queue for the next pass
        if (hasMoreCharacters)
            tokens.push(antlrFactory.create(antlrSource, CONTENT, host.index(), end - 1, 1, 0, host))
    }

    private fun handleComment(opener: AntlrToken, body: AntlrToken, closer: AntlrToken) {
        val comment = ScopeBuilder.Comment(opener, body, closer)
        val (marker, definition) = tryParseComment(body) ?: return run {
            // If open/closed - add there, otherwise add and leave the scope
            val scope = if (builder.acceptsAny()) builder else stack.removeLast()
            scope += comment
        }

        val code = ScopeBuilder.Code(comment, marker, definition)
        handleCode(code)
    }

    private fun handleCode(code: ScopeBuilder.Code): Unit = when {
        code.type == INDEPENDENT ->
            builder += code

        builder is ScopeBuilder.Root -> {
            builder += code
            // '}' shouldn't be possible in the root scope
            if (code.definition.closer != null)
                problems.at(code.definition.closer!!) report problem { "Unmatched scope closer" }

            // Add invalid extensions to the scope stack anyway
            if (code.type != CLOSER)
                stack.addLast(code)
            Unit
        }

        else -> {
            val parent = builder as ScopeBuilder.Code
            // Check for situations like `? if condition { ... $}`, in which case we close it anyway
            if (code.type.isExtension && parent.kind != code.kind)
                problems.at(code.marker) report problem { "Extension closes unmatched ${parent.kind.scopeType()} scope" }

            // Occurs if we have an unfinished open scope, in which case we close it prematurely
            if (code.type.isExtension && parent.type.isOpen)
                problems.at(code.marker) report problem { "Extension closes an open scope" }

            if (code.type.isExtension) stack.removeLast()
            builder += code
            if (!code.type.isEmpty) stack.addLast(code)
            Unit
        }
    }

    private fun tryParseComment(body: AntlrToken): Pair<AntlrToken, DefinitionToken>? {
        if (body.stopIndex < body.startIndex)
            return null // Empty comment body

        val input = body.text.toStream()
        when (input.LC(1)) {
            '?', '$', '~' -> {}
            else -> return null
        }

        val listener = InlineErrorListener(problems, FileLineIndex(input), body.startIndex)
        val lexer = StitcherLexer(input).errorListener(listener)
        val parser = StitcherParser(InlineTokenStream(lexer, body.startIndex)).errorListener(listener)
        return parser.definition().accept(visitor)
    }

    companion object {
        const val CONTENT: Int = 1
        const val COMMENT_OPEN: Int = 2
        const val COMMENT_BODY: Int = 3
        const val COMMENT_CLOSE: Int = 4

        @JvmField val TOKEN_NAMES: Array<String?> = arrayOf(null, "CONTENT", "COMMENT_OPEN", "COMMENT_BODY", "COMMENT_CLOSE")
        @JvmField val VOCABULARY: Vocabulary = VocabularyImpl(emptyArray(), TOKEN_NAMES)

        fun parse(stream: TokenStream, problems: ProblemSink, factory: StitcherTokenFactory): BlockToken.Root =
            LayoutParser(stream, problems, factory).collect()
    }
}

private class TokenQueue(val stream: TokenStream, val buffer: Deque<AntlrToken> = ArrayDeque(4)) {
    val current: AntlrToken get() = if (buffer.isNotEmpty()) buffer.peekFirst() else stream.LT(1)
    fun hasMore(): Boolean = buffer.isNotEmpty() || (stream.LA(1) != EOF)
    fun push(token: AntlrToken) = buffer.addLast(token)
    fun consume(): AntlrToken = current.also {
        if (buffer.isNotEmpty()) buffer.removeFirst() else stream.consume()
    }
}

private interface ContainerScopeBuilder : ScopeBuilder {
    operator fun plusAssign(entry: ScopeBuilder)
    fun acceptsAny(): Boolean
}

private sealed interface ScopeBuilder {
    fun build(factory: StitcherTokenFactory): BlockToken

    class Root(
        private val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ContainerScopeBuilder {
        override fun plusAssign(entry: ScopeBuilder) = when (val last = entries.lastOrNull()) {
            is Content if (entry is Content) -> last += entry
            else -> entries += entry
        }

        override fun acceptsAny(): Boolean = true
        override fun build(factory: StitcherTokenFactory): BlockToken.Root =
            BlockToken.Root(entries.map { it.build(factory) })
    }

    class Code(
        private val host: Comment,
        val marker: AntlrToken,
        val definition: DefinitionToken,
        private val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ContainerScopeBuilder {
        val kind: Int get() = marker.type
        val type: DefinitionType get() = definition.type

        override fun plusAssign(entry: ScopeBuilder) = when (val last = entries.lastOrNull()) {
            is Content if (entry is Content) -> last += entry
            else -> entries += entry
        }

        override fun acceptsAny(): Boolean = type.isScoped

        override fun build(factory: StitcherTokenFactory): BlockToken.Code = BlockToken.Code(
            host.build(factory), factory.fromAntlrToken(marker), definition, entries.map { it.build(factory) }
        )
    }

    class Content(
        private val tokens: MutableList<AntlrToken>
    ) : ScopeBuilder {
        constructor(token: AntlrToken) : this(mutableListOf(token))
        private var blank: Boolean = true

        override fun build(factory: StitcherTokenFactory): BlockToken.Content {
            check(tokens.isNotEmpty()) { "Unable to build an empty content block" }
            val range = merge(tokens.first().range, tokens.last().range)
            val text = tokens.joinToString("", transform = AntlrToken::getText)
            val leaf = factory.create(LeafToken.Type(StitcherLexer.IDENTIFIER), range, text)
            return BlockToken.Content(leaf, blank)
        }

        operator fun plusAssign(entry: Content) {
            if (entry.tokens.any { it.text.isNotBlank() }) blank = false
            tokens += entry.tokens
        }

        fun isBlank(): Boolean = blank
    }

    class Comment(
        private val opener: AntlrToken,
        private val body: AntlrToken,
        private val closer: AntlrToken
    ) : ScopeBuilder {
        override fun build(factory: StitcherTokenFactory): BlockToken.Comment = BlockToken.Comment(
            factory.fromAntlrToken(opener), factory.fromAntlrToken(body), factory.fromAntlrToken(closer)
        )
    }
}