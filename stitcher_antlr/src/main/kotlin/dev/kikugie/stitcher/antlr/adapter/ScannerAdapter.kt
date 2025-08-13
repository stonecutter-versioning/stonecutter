package dev.kikugie.stitcher.antlr.adapter

import dev.kikugie.commons.collections.FixedQueue
import dev.kikugie.commons.then
import dev.kikugie.stitcher.antlr.LayoutParser
import dev.kikugie.stitcher.antlr.StitcherLightBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherLightLexer
import dev.kikugie.stitcher.antlr.StitcherLightParser
import dev.kikugie.stitcher.util.get
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.Vocabulary
import org.antlr.v4.runtime.misc.Pair

class ScannerAdapter(private val scanner: Lexer, private val openers: IntArray, private val closers: IntArray) : TokenSource by scanner {
    fun interface Factory { fun create(input: CharStream): ScannerAdapter }
    private val queue: FixedQueue<Token> = FixedQueue(4)
    private var checkpoint: Checkpoint = Checkpoint(0, 0, 0, false)

    override fun nextToken(): Token = when(queue.size) {
        0 -> readNextTokens() then queue.element()!!
        1 -> readNextTokens() then removeAndPeek()
        else -> removeAndPeek()
    }

    override fun getLine(): Int =
        if (scanner._hitEOF) scanner.line else queue.peek()?.line ?: 0

    override fun getCharPositionInLine(): Int =
        if (scanner._hitEOF) scanner.charPositionInLine else queue.peek()?.charPositionInLine ?: 0

    private fun removeAndPeek() =
        queue.poll() then queue.element()!!

    private fun readNextTokens(): Boolean {
        if (scanner._hitEOF) throw NoSuchElementException()

        val next = scanner.nextToken()
        if (next.type == Token.EOF) {
            if (checkpoint.comment) {
                addComment(next)
                queue.add(token(LayoutParser.COMMENT_CLOSE, next))
            } else if (checkpoint.cursor < next.startIndex)
                addContent(next)

            return queue.add(next)
        }

        if (next.type in openers) {
            if (checkpoint.cursor < next.startIndex)
                addContent(next)
            checkpoint = Checkpoint(next, true)
            return queue.add(token(LayoutParser.COMMENT_OPEN, next))
        }

        if (next.type in closers) {
            addComment(next)
            checkpoint = Checkpoint(next, false)
            return queue.add(token(LayoutParser.COMMENT_CLOSE, next))
        }

        val message = """
            Token ${scanner.vocabulary.getDisplayName(next.type)} of ${scanner.grammarFileName} is not a valid comment marker.
            Comment scanners should only yield tokens matching comment start and end.
        """.trimIndent()
        throw RecognitionException(message, scanner, inputStream, null)
    }

    private fun addContent(next: Token) =
        queue.add(token(LayoutParser.CONTENT, next.startIndex))

    private fun addComment(next: Token) =
        queue.add(token(quickCheckCommentScope(checkpoint.cursor, next.startIndex), next.startIndex))

    // FIXME: split the scope range
    private fun quickCheckCommentScope(start: Int, end: Int): Int {
        val text = scanner.inputStream[start, end]
        if (!(text.startsWith('?') || text.startsWith('$') || text.startsWith('~')))
            return LayoutParser.COMMENT_BODY

        val lexer = StitcherLightLexer(CharStreams.fromString(text))
        val parser = StitcherLightParser(CommonTokenStream(lexer))
        val definition = parser.definition()
        return definition.accept(LightScopeResolver)
    }

    private fun token(type: Int, endExclusive: Int): Token =
        tokenFactory.create(Pair(this, inputStream), type, null, Token.DEFAULT_CHANNEL,
            checkpoint.cursor, endExclusive - 1, checkpoint.line, checkpoint.offset)

    private fun token(type: Int, host: Token): Token =
        tokenFactory.create(Pair(this, inputStream), type, null, Token.DEFAULT_CHANNEL,
            host.startIndex, host.stopIndex, host.line, host.charPositionInLine)

    private fun Checkpoint(token: Token, comment: Boolean): Checkpoint {
        val text = token.text
        return if (text.endsWith('\n') || text.endsWith('\r'))
            Checkpoint(token.stopIndex + 1, token.line + 1, 0, comment)
        else
            Checkpoint(token.stopIndex + 1, token.line, token.charPositionInLine + text.length, comment)
    }
    private data class Checkpoint(val cursor: Int, val line: Int, val offset: Int, val comment: Boolean)

    private object LightScopeResolver : StitcherLightBaseVisitor<Int>() {
        override fun visitDefinition(ctx: StitcherLightParser.DefinitionContext): Int {
            val closer = ctx.SCOPE_CLOSE() != null
            val opener = ctx.SCOPE_OPEN() != null
            val empty = ctx.CONTENT().isNullOrEmpty()

            if (ctx.REPL_MARK() != null)
                return LayoutParser.REPLACEMENT

            if (ctx.SWAP_MARK() != null) return when {
                closer -> LayoutParser.SWAP_CLOSER
                opener -> LayoutParser.SWAP_OPENER
                else -> LayoutParser.SWAP_FREE_OPENER
            }

            if (ctx.COND_MARK() != null) return when {
                !closer && opener -> LayoutParser.CONDITION_OPENER
                !closer -> LayoutParser.CONDITION_FREE_OPENER
                closer && opener -> LayoutParser.CONDITION_EXTENSION
                closer && !empty -> LayoutParser.CONDITION_FREE_EXTENSION
                else -> LayoutParser.CONDITION_CLOSER
            }

            return LayoutParser.COMMENT_BODY
        }
    }
}