package dev.kikugie.stitcher.antlr.layout

import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.adapter.InlineCharStream
import dev.kikugie.stitcher.antlr.adapter.InlineTokenFactory
import dev.kikugie.stitcher.antlr.converter.DefinitionBuilder
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.ScopeType.*
import dev.kikugie.stitcher.util.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

private val SKIPPABLE_SPACES = charArrayOf(' ', '\t', '\r', '\n')

internal class LayoutBuilder(private val stream: TokenStream) {
    private val blocks: Deque<BlockToken> = ArrayDeque()
    private val walker: ScopeStackWalker = ScopeStackWalker()

    private val current: Token
        get() = stream.LT(1)

    fun build(): BlockToken.Root {
        while (stream.LA(1) != IntStream.EOF) next()
        return BlockToken.Root(blocks.toList())
    }

    private fun next() = when (current.type) {
        LayoutTokens.CONTENT -> handleContent()
        LayoutTokens.COMMENT_OPEN -> handleCommentEntry()
        else -> TODO("Should be impossible to trigger")
    }

    private fun handleContent() = advancing {
        content(it)
        updateStack()
    }

    private fun handleCommentEntry() = advancing { opener ->
        val definition = advancing { body ->
            definition(body)?.let { return@advancing it }
            comment(opener, body, consume())
            updateStack()
            return
        }

        var marker: TerminalNode
        var context: ParserRuleContext

        with(definition) {
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

        advancing { code(marker, context) }
        updateStack()
    }

    private fun consume(): Token = advancing { it }
    private inline fun <T> advancing(action: (Token) -> T): T =
        current.let { stream.consume(); action(it) }

    private fun content(token: Token) {
        blocks += BlockToken.Content(token.range, token.inputStream)
    }

    private fun comment(opener: Token, body: Token, closer: Token) {
        blocks += BlockToken.Comment(BlockToken.Content(body.range, body.inputStream), opener.startIndex..closer.stopIndex)
    }

    private fun code(marker: TerminalNode, context: ParserRuleContext) {
        blocks += BlockToken.Code(marker.toLeaf(), context.accept(DefinitionBuilder))
    }

    private fun updateStack() {
        blocks.removeLast().accept(walker)
    }

    private fun definition(token: Token): StitcherParser.DefinitionContext? {
        if (token.stopIndex < token.startIndex)
            return null // Empty comment body

        return InlineCharStream(token).use {
            when (it.LA(1).toChar()) {
                '?', '$', '~' -> Unit // Continue
                else -> return@use null
            }

            val lexer = StitcherLexer(it).apply {
                tokenFactory = InlineTokenFactory(token)
            }
            val parser = StitcherParser(CommonTokenStream(lexer))
            parser.definition()
        }
    }

    private inner class ScopeStackWalker : BlockToken.Visitor<Unit> {
        override fun visitContent(it: BlockToken.Content) = when (val last = blocks.last) {
            is BlockToken.Code if (last.definition.type != CLOSED) -> appendContent(it, last)
            else -> blocks += it
        }

        override fun visitComment(it: BlockToken.Comment) = when (val last = blocks.last) {
            is BlockToken.Code if (last.definition.type != CLOSED) -> appendComment(it, last)
            else -> blocks += it
        }

        override fun visitCode(it: BlockToken.Code) {
            blocks += if (it.definition.closer == null) it
            else it.copy(scope = collectScope(it))
        }

        override fun visitRoot(it: BlockToken.Root) {
            error("Shouldn't be called on root")
        }

        private fun appendComment(it: BlockToken.Comment, last: BlockToken.Code) {
            blocks.replaceLast(last.copy(scope = listOf(it)))
        }

        private fun appendContent(it: BlockToken.Content, last: BlockToken.Code) {
            InlineCharStream(it).use { stream ->
                if (!stream.skipLeadingSpaces(*SKIPPABLE_SPACES)) {
                    // The entire block is whitespace - there's nothing to comment
                    blocks += it; return@use
                }

                val scopeStart = stream.index() + stream.start
                val hasMore = stream.consumeScope(last)
                val scopeEnd = stream.index() + stream.start
                val scopeContent =
                    if (!hasMore) it
                    else BlockToken.Content(scopeStart..<scopeEnd, it.source)
                blocks.replaceLast(last.copy(scope = listOf(scopeContent)))

                if (hasMore)
                    blocks += BlockToken.Content(scopeEnd..<stream.end, it.source)
            }
        }

        private fun collectScope(ref: BlockToken.Code): List<BlockToken> = buildList {
            while (true) {
                val it = blocks.removeLast()
                if (it !is BlockToken.Code) {
                    this += it; continue
                }

                if (it.marker.type != ref.marker.type)
                    error("TODO: make an error for this")

                if (it.definition.type != CLOSED)
                    error("TODO: make an error for this")

                break
            }
        }

        private fun CharStream.consumeScope(code: BlockToken.Code) = when (code.definition.type) {
            LINE -> skipWhile {
                it != '\n' && it != '\r'
            }

            WORD -> skipWhile {
                it !in SKIPPABLE_SPACES
            }

            else -> LA(1) != IntStream.EOF
        }
    }
}