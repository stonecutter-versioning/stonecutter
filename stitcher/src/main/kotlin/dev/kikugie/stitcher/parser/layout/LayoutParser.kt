package dev.kikugie.stitcher.parser.layout

import dev.kikugie.stitcher.antlr.*
import dev.kikugie.stitcher.data.composite.DefinitionToken
import dev.kikugie.stitcher.data.composite.DefinitionToken.Type.CLOSER
import dev.kikugie.stitcher.data.composite.DefinitionToken.Type.INDEPENDENT
import dev.kikugie.stitcher.data.composite.RootBlock
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.component.DefinitionBuilder
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Token.EOF
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.Vocabulary
import org.antlr.v4.runtime.VocabularyImpl
import java.util.*

/**
 * Parses a [stream] of comment tokens into an AST.
 *
 * The tree consists of two component types:
 * - Blocks: content, comments, and code comments;
 * - Scopes: collections of blocks.
 *
 * The parser works with a scope stack, pushing incoming blocks to it.
 * The scope may accept the block or reject it, which means it should be closed.
 */
internal class LayoutParser private constructor(
    val stream: TokenStream,
    val source: CharStream,
    val problems: ProblemSource,
    val factory: StitcherTokenFactory
) : ProblemSource by problems {
    private val stack: Deque<BlockBuilder.Scoped> = ArrayDeque(4)
    private val visitor: StitcherVisitor<Pair<AntlrToken, DefinitionToken>> = DefinitionBuilder.paired(problems, factory)

    init {
        stack += RootBuilder(factory)
    }

    private fun collect(): RootBlock {
        consumeTokenStream()
        return finalizeUnfinishedScopes()
    }

    private fun consumeTokenStream() {
        while (stream.LA(1) != EOF) when (stream.LA(1)) {
            CONTENT -> handleContent(stream.advance())
            COMMENT_OPEN -> handleComment(stream.advance(), stream.advance(), stream.advance())
            else -> at(stream.advance()) report "Unexpected token"
        }
    }

    private fun finalizeUnfinishedScopes(): RootBlock {
        while (stack.isNotEmpty()) when (val it = stack.removeLast()) {
            is RootBuilder -> return it.build()
            is CodeBuilder -> it.finalize(problems, true)
        }
        error("Root scope was consumed")
    }

    private fun acceptBlock(block: BlockBuilder): Unit = when (val result = stack.peekLast().accept(block)) {
        // Can accept more - do nothing
        BlockAcceptResult.ConsumedOpen -> Unit

        // Pop the current scope
        BlockAcceptResult.ConsumedFinal ->
            stack.removeLast().finalize(problems, false)

        // Pop the current scope and push to the next
        BlockAcceptResult.Rejected -> {
            stack.removeLast().finalize(problems, false)
            acceptBlock(block)
        }

        // Pop the current scope and push remainder to the next
        is BlockAcceptResult.ConsumedPartial -> {
            stack.removeLast().finalize(problems, false)
            acceptBlock(result.remainder)
        }
    }

    private fun handleContent(token: AntlrToken) {
        if (token.text.isNotEmpty()) acceptBlock(ContentBuilder(factory, token))
    }

    private fun handleComment(opener: AntlrToken, body: AntlrToken, closer: AntlrToken) {
        val comment = CommentBuilder(factory, opener, body, closer)
        val (marker, definition) = parseCommentBody(body)
            ?: return acceptBlock(comment)

        val code = CodeBuilder(factory, comment, marker, definition)
        if (code.type == INDEPENDENT) acceptBlock(code)
        else handleCode(code)
    }

    private fun handleCode(code: CodeBuilder): Unit = when (val parent = stack.peekLast()) {
        is RootBuilder -> handleRootCode(code)
        is CodeBuilder -> handleNestedCode(code, parent)
    }

    private fun LayoutParser.handleRootCode(code: CodeBuilder) {
        acceptBlock(code)
        // '}' shouldn't be possible in the root scope
        if (code.definition.closer != null)
            at(code.definition.closer!!) report "Unmatched scope closer"

        // Add invalid extensions to the scope stack anyway
        if (code.type != CLOSER) stack.addLast(code) else Unit
    }

    private fun LayoutParser.handleNestedCode(
        code: CodeBuilder,
        parent: CodeBuilder
    ) {
        // Check for situations like `? if condition { ... $}`, in which case we close it anyway
        if (code.type.isExtension && parent.kind != code.kind)
            at(code.marker) report "Extension closes unmatched ${parent.kind.scopeType()} scope"

        // Occurs if we have an unfinished open scope, in which case we close it prematurely
        if (code.type.isExtension && parent.type.isOpen)
            at(code.marker) report "Extension closes an open scope"

        if (code.type.isExtension)
            stack.removeLast().finalize(problems, false)
        acceptBlock(code)
        if (!code.type.isEmpty) stack.addLast(code) else Unit
    }

    private fun parseCommentBody(body: AntlrToken): Pair<AntlrToken, DefinitionToken>? {
        if (body.stopIndex < body.startIndex)
            return null // Empty comment body

        val input = body.text.toStream()
        when (input.LC(1)) {
            '?', '$', '~' -> {}
            else -> return null
        }

        val listener = InlineErrorListener(problems, FileLineIndex(input), body.startIndex)
        val scanner = StitcherLexer(input).errorListener(listener)
        val stream = InlineTokenStream(scanner, source, body.startIndex, at(body.startIndex))
        val parser = StitcherParser(stream).errorListener(listener)
        return parser.definition().accept(visitor)
    }

    companion object {
        const val CONTENT: Int = 1
        const val COMMENT_OPEN: Int = 2
        const val COMMENT_BODY: Int = 3
        const val COMMENT_CLOSE: Int = 4

        @JvmField val TOKEN_NAMES: Array<String?> = arrayOf(null, "CONTENT", "COMMENT_OPEN", "COMMENT_BODY", "COMMENT_CLOSE")
        @JvmField val VOCABULARY: Vocabulary = VocabularyImpl(emptyArray(), TOKEN_NAMES)

        fun parse(stream: TokenStream, source: CharStream, problems: ProblemSource, factory: StitcherTokenFactory): RootBlock =
            LayoutParser(stream, source, problems, factory).collect()
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