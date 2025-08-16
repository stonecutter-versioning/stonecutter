package dev.kikugie.stitcher.antlr.layout

import dev.kikugie.stitcher.antlr.StitcherLexer
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.antlr.adapter.InlineCharStream
import dev.kikugie.stitcher.antlr.adapter.InlineTokenFactory
import dev.kikugie.stitcher.antlr.converter.DefinitionBuilder
import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.util.range
import dev.kikugie.stitcher.util.toLeaf
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.ArrayDeque
import java.util.Deque
import kotlin.text.compareTo


internal class LayoutBuilder(private val stream: TokenStream) {
    private val blocks: Deque<BlockToken> = ArrayDeque()

    private val current: Token
        get() = stream.LT(1)

    fun build() {

    }

    private fun next() = when (current.type) {
        LayoutTokens.CONTENT -> handleContent()
        LayoutTokens.COMMENT_OPEN -> handleCommentEntry()
        else -> TODO()
    }

    private fun handleContent() = advancing {
        val last: BlockToken? = blocks.pollLast()
        if (last !is BlockToken.Code) return content(it)

        // TODO: handle block splitting
    }

    private fun handleCommentEntry() = advancing { opener ->
        val definition = advancing { body ->
            definition(body)?.let { return@advancing it }
            return comment(opener, body, consume())
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
        updateBlockStack(blocks.last() as BlockToken.Code)
    }

    private fun updateBlockStack(code: BlockToken.Code) {

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

    private fun definition(token: Token): StitcherParser.DefinitionContext? {
        if (token.stopIndex < token.startIndex)
            return null // Empty comment body

        return InlineCharStream(token).use {
            when (it.LA(1).toChar()) {
                '?', '$', '~' -> { /* continue */ }
                else -> return@use null
            }

            val lexer = StitcherLexer(it).apply {
                tokenFactory = InlineTokenFactory(token)
            }
            val parser = StitcherParser(CommonTokenStream(lexer))
            parser.definition()
        }
    }
}