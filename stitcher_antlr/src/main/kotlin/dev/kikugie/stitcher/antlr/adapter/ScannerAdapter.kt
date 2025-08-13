package dev.kikugie.stitcher.antlr.adapter

import dev.kikugie.commons.collections.FixedQueue
import dev.kikugie.commons.then
import dev.kikugie.stitcher.antlr.LayoutParser
import dev.kikugie.stitcher.antlr.StitcherLightBaseVisitor
import dev.kikugie.stitcher.antlr.StitcherLightLexer
import dev.kikugie.stitcher.antlr.StitcherLightParser
import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.util.get
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.LexerNoViableAltException
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.Vocabulary
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.misc.Pair

class ScannerAdapter(private val scanner: Lexer, private val openers: IntArray, private val closers: IntArray) : TokenSource by scanner {
    fun interface Factory { fun create(input: CharStream): ScannerAdapter }
    private val queue: FixedQueue<Token> = FixedQueue(4)
    private var checkpoint: Checkpoint = Checkpoint(0, 0, 0, false)

    private var scope: ScopeType = NONE

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

    private fun readNextTokens() {
        if (checkpoint.line < 0) throw NoSuchElementException()

        val next = scanner.nextToken()
        if (next.type == Token.EOF) {
            if (checkpoint.comment) {
                addComment(next)
                push(LayoutParser.COMMENT_CLOSE, next)
            } else if (checkpoint.cursor < next.startIndex)
                addContent(next)

            checkpoint = Checkpoint(0, -1, 0, false)
            queue.add(next)
            return
        }

        if (next.type in openers) {
            if (checkpoint.cursor < next.startIndex)
                addContent(next)
            else
                addEmpty()
            checkpoint = Checkpoint(next, true)
            return push(LayoutParser.COMMENT_OPEN, next)
        }

        if (next.type in closers) {
            addComment(next)
            checkpoint = Checkpoint(next, false)
            return push(LayoutParser.COMMENT_CLOSE, next)
        }

        val message = """
            Token ${scanner.vocabulary.getDisplayName(next.type)} of ${scanner.grammarFileName} is not a valid comment marker.
            Comment scanners should only yield tokens matching comment start and end.
        """.trimIndent()
        throw UnregisteredTokenException(message, scanner, inputStream, next)
    }

    private fun addEmpty() = when(scope) {
        LINE, WORD -> { scope = NONE }
        else -> {}
    }

    // TODO: this shit is too ass but it works, future me please clean this up
    private fun addContent(next: Token) = when(scope) {
        NONE, CLOSED -> push(LayoutParser.CONTENT, next)
        LINE -> {
            val contentText = scanner.inputStream[checkpoint.cursor, next.startIndex]
            val regionEnd = contentText.run {
                var seenRealText = false
                indexOfFirst {
                    when (it) {
                        ' ', '\t' -> {
                            seenRealText = true; false
                        }
                        '\n', '\r' -> seenRealText
                        else -> false
                    }
                }
            }

            if (regionEnd < 0)
                push(LayoutParser.CONTENT, next)
            else {
                push(LayoutParser.CONTENT, checkpoint.cursor + regionEnd)
                push(LayoutParser.CONTENT, checkpoint.cursor + regionEnd, next.stopIndex + 1)
            }
        }
        WORD -> {
            val contentText = scanner.inputStream[checkpoint.cursor, next.startIndex]
            val regionEnd = contentText.run {
                var seenRealText = false
                indexOfFirst {
                    when (it) {
                        ' ', '\t' -> if (seenRealText) true else {
                            seenRealText = true; false
                        }
                        else -> false
                    }
                }
            }

            if (regionEnd < 0)
                push(LayoutParser.CONTENT, next)
            else {
                push(LayoutParser.CONTENT, checkpoint.cursor + regionEnd)
                push(LayoutParser.CONTENT, checkpoint.cursor + regionEnd, next.stopIndex + 1)
            }
        }
    }

    private fun addComment(next: Token) {
        val commentEnd = next.startIndex
        val contentText = scanner.inputStream[checkpoint.cursor, commentEnd]
        if (!(contentText.startsWith('?') || contentText.startsWith('$') || contentText.startsWith('~'))) {
            push(LayoutParser.COMMENT_BODY, commentEnd)
            checkpoint = Checkpoint(next, false)
            return
        }

        val lexer = StitcherLightLexer(CharStreams.fromString(contentText))
        val parser = StitcherLightParser(CommonTokenStream(lexer))
        val definition = parser.definition()
        val (type, scope) = definition.accept(LightScopeResolver)

        push(type, commentEnd)
        if (scope != NONE) this.scope = scope
    }

    private fun push(type: Int, endExclusive: Int) =
        push(type, checkpoint.cursor, endExclusive)

    private fun push(type: Int, start: Int, endExclusive: Int) {
        queue.add(tokenFactory.create(Pair(this, inputStream), type, null, Token.DEFAULT_CHANNEL,
            start, endExclusive - 1, checkpoint.line, checkpoint.offset))
    }

    private fun push(type: Int, host: Token) {
        queue.add(tokenFactory.create(Pair(this, inputStream), type, null, Token.DEFAULT_CHANNEL,
            host.startIndex, host.stopIndex, host.line, host.charPositionInLine))
    }

    private fun Checkpoint(token: Token, comment: Boolean): Checkpoint {
        val text = token.text
        return if (text.endsWith('\n') || text.endsWith('\r'))
            Checkpoint(token.stopIndex + 1, token.line + 1, 0, comment)
        else
            Checkpoint(token.stopIndex + 1, token.line, token.charPositionInLine + text.length, comment)
    }
    private data class Checkpoint(val cursor: Int, val line: Int, val offset: Int, val comment: Boolean)

    private enum class ScopeType { NONE, CLOSED, LINE, WORD }

    private object LightScopeResolver : StitcherLightBaseVisitor<kotlin.Pair<Int, ScopeType>>() {
        override fun visitDefinition(ctx: StitcherLightParser.DefinitionContext): kotlin.Pair<Int, ScopeType> {
            val closer = ctx.SCOPE_CLOSE() != null
            val opener = ctx.SCOPE_OPEN() != null
            val word = ctx.SCOPE_WORD() != null
            val empty = ctx.CONTENT().isNullOrEmpty()

            if (ctx.REPL_MARK() != null)
                return LayoutParser.REPLACEMENT to ScopeType.NONE

            if (ctx.SWAP_MARK() != null) return when {
                closer -> LayoutParser.SWAP_CLOSER to ScopeType.NONE
                opener -> LayoutParser.SWAP_OPENER to ScopeType.CLOSED
                else -> LayoutParser.SWAP_FREE_OPENER to scope(word)
            }

            if (ctx.COND_MARK() != null) return when {
                !closer && opener -> LayoutParser.CONDITION_OPENER to ScopeType.CLOSED
                !closer -> LayoutParser.CONDITION_FREE_OPENER to scope(word)
                closer && opener -> LayoutParser.CONDITION_EXTENSION to ScopeType.CLOSED
                closer && !empty -> LayoutParser.CONDITION_FREE_EXTENSION to scope(word)
                else -> LayoutParser.CONDITION_CLOSER to ScopeType.NONE
            }

            return LayoutParser.COMMENT_BODY to ScopeType.NONE
        }

        private fun scope(word: Boolean): ScopeType = when {
            word -> ScopeType.WORD
            else -> ScopeType.LINE
        }
    }
}