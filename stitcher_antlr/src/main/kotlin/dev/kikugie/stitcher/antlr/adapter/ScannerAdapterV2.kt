package dev.kikugie.stitcher.antlr.adapter

import dev.kikugie.commons.collections.FixedQueue
import dev.kikugie.commons.collections.plusAssign
import dev.kikugie.commons.collections.first
import dev.kikugie.commons.then
import dev.kikugie.stitcher.antlr.LayoutParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair

class ScannerAdapterV2(private val scanner: Lexer, private val openers: IntArray, private val closers: IntArray) : TokenSource by scanner {
    fun interface Factory {
        fun create(input: CharStream): ScannerAdapterV2
    }

    private data class Checkpoint(val cursor: Int, val line: Int, val offset: Int, val comment: Boolean)

    private val queue: FixedQueue<Token> = FixedQueue(3)
    private var checkpoint: Checkpoint = Checkpoint(0, 0, 0, false)

    override fun getLine(): Int =
        if (scanner._hitEOF) scanner.line else queue.peek()?.line ?: 0

    override fun getCharPositionInLine(): Int =
        if (scanner._hitEOF) scanner.charPositionInLine else queue.peek()?.charPositionInLine ?: 0

    override fun nextToken(): Token = when (queue.size) {
        0 -> advance() then queue.first()
        1 -> advance() then next()
        else -> next()
    }

    private fun next() = queue.run { remove(); first() }

    private fun advance() {
        if (checkpoint.line < 0) throw NoSuchElementException()

        val next = scanner.nextToken()
        when (next.type) {
            Token.EOF -> handleEOF(next)
            in openers -> handleCommentStart(next)
            in closers -> handleCommentEnd(next)
            else -> reportUnknown(next)
        }
    }

    private fun handleEOF(token: Token) {
        if (checkpoint.comment) handleCommentEnd(token)
        else if (checkpoint.cursor < token.startIndex)
            push(LayoutParser.CONTENT, token.startIndex)
        queue += token
        checkpoint = Checkpoint(-1, -1, -1, false)
    }

    private fun handleCommentStart(token: Token) {
        // TODO: register a warning if we're already in a comment
        if (checkpoint.comment) return
        if (checkpoint.cursor < token.startIndex)
            push(LayoutParser.CONTENT, token.startIndex)
        push(LayoutParser.COMMENT_OPEN, token)
        checkpoint = Checkpoint(token, true)
    }

    private fun handleCommentEnd(token: Token) {
        // TODO: register a warning if we're outside a comment
        if (!checkpoint.comment) return
        // TODO: should it really possibly create an empty token?
        push(LayoutParser.COMMENT_BODY, token.startIndex)
        push(LayoutParser.COMMENT_CLOSE, token)
        checkpoint = Checkpoint(token, false)
    }

    private fun reportUnknown(token: Token) {
        // TODO: register a warning for it
    }

    private fun push(type: Int, endExclusive: Int) =
        push(type, checkpoint.cursor, endExclusive)

    private fun push(type: Int, start: Int, endExclusive: Int) {
        queue += tokenFactory.create(
            Pair(this, inputStream), type, null, Token.DEFAULT_CHANNEL,
            start, endExclusive - 1, checkpoint.line, checkpoint.offset
        )
    }

    private fun push(type: Int, host: Token) {
        queue += tokenFactory.create(
            Pair(this, inputStream), type, null, Token.DEFAULT_CHANNEL,
            host.startIndex, host.stopIndex, host.line, host.charPositionInLine
        )
    }

    private fun Checkpoint(token: Token, comment: Boolean): Checkpoint =
        Checkpoint(token.stopIndex + 1, scanner.line, scanner.charPositionInLine, comment)
}