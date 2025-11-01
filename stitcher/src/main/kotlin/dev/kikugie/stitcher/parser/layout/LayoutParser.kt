package dev.kikugie.stitcher.parser.layout

import dev.kikugie.commons.takeAsOrNull
import dev.kikugie.stitcher.antlr.*
import dev.kikugie.stitcher.data.composite.DefinitionToken
import dev.kikugie.stitcher.data.composite.DefinitionToken.Type.CLOSER
import dev.kikugie.stitcher.data.composite.DefinitionToken.Type.INDEPENDENT
import dev.kikugie.stitcher.data.composite.RootBlock
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.component.DefinitionBuilder
import dev.kikugie.stitcher.parser.layout.ScopeBuilder.ContainerScopeBuilder
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.Token.EOF
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.Vocabulary
import org.antlr.v4.runtime.VocabularyImpl
import java.util.*

// TODO: Check scopes in comments and add a split.
internal class LayoutParser private constructor(val stream: TokenStream, val sink: ProblemSink, val factory: StitcherTokenFactory) : ProblemSource by sink {
    private val stack: Deque<ContainerScopeBuilder> = ArrayDeque<ContainerScopeBuilder>(4).apply { push(ScopeBuilder.Root()) }
    private val visitor: StitcherVisitor<Pair<AntlrToken, DefinitionToken>> = DefinitionBuilder.paired(sink, factory)

    private val builder: ContainerScopeBuilder get() = stack.peekLast()
    private val antlrSource: TokenSource get() = stream.tokenSource

    private fun collect(): RootBlock {
        while (stream.LA(1) != EOF) when (stream.LA(1)) {
            CONTENT -> handleContent(stream.advance())
            COMMENT_OPEN -> handleComment(stream.advance(), stream.advance(), stream.advance())
            else -> at(stream.advance()) report "Unexpected token"
        }

        while (stack.isNotEmpty()) when (val it = stack.removeLast()) {
            is ScopeBuilder.Root -> return it.build(factory)
            is ScopeBuilder.Code -> it.checkUnfinished(sink, false)
        }
        error("Root scope was consumed")
    }

    private fun closeBlock() {
        stack.removeLast().takeAsOrNull<ScopeBuilder.Code>()
            ?.checkUnfinished(sink, true)
    }

    private fun acceptBlock(block: ScopeBuilder): Unit = when (val result = builder.tryAccept(block, antlrSource)) {
        AcceptResult.ConsumedOpen -> Unit
        AcceptResult.ConsumedFinal -> closeBlock()
        AcceptResult.Rejected -> {
            check(stack.size > 1) { "Failed to accept content block" }
            closeBlock()
            acceptBlock(block)
        }

        is AcceptResult.ConsumedPartial -> {
            closeBlock()
            acceptBlock(result.remaining)
        }
    }

    private fun handleContent(token: AntlrToken) {
        if (token.text.isNotEmpty()) acceptBlock(ScopeBuilder.Content(token))
    }

    private fun handleComment(opener: AntlrToken, body: AntlrToken, closer: AntlrToken) {
        val comment = ScopeBuilder.Comment(opener, body, closer)
        val (marker, definition) = tryParseComment(body)
            ?: return acceptBlock(comment)

        val code = ScopeBuilder.Code(comment, marker, definition)
        handleCode(code)
    }

    private fun handleCode(code: ScopeBuilder.Code): Unit = when {
        code.type == INDEPENDENT -> acceptBlock(code)

        builder is ScopeBuilder.Root -> {
            acceptBlock(code)
            // '}' shouldn't be possible in the root scope
            if (code.definition.closer != null)
                at(code.definition.closer!!) report "Unmatched scope closer"

            // Add invalid extensions to the scope stack anyway
            if (code.type != CLOSER)
                stack.addLast(code)
            Unit
        }

        else -> {
            val parent = builder as ScopeBuilder.Code
            // Check for situations like `? if condition { ... $}`, in which case we close it anyway
            if (code.type.isExtension && parent.kind != code.kind)
                at(code.marker) report "Extension closes unmatched ${parent.kind.scopeType()} scope"

            // Occurs if we have an unfinished open scope, in which case we close it prematurely
            if (code.type.isExtension && parent.type.isOpen)
                at(code.marker) report "Extension closes an open scope"

            if (code.type.isExtension) closeBlock()
            acceptBlock(code)
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

        val listener = InlineErrorListener(sink, FileLineIndex(input), body.startIndex)
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

        fun parse(stream: TokenStream, problems: ProblemSink, factory: StitcherTokenFactory): RootBlock =
            LayoutParser(stream, problems, factory).collect()
    }
}

private fun TokenStream.advance(): AntlrToken =
    LT(1).also { consume() }

private fun Int.scopeType(): String = when (this) {
    StitcherLexer.COND_MARK -> "condition"
    StitcherLexer.SWAP_MARK -> "swap"
    StitcherLexer.REPL_MARK -> "replacement"
    else -> "unknown"
}