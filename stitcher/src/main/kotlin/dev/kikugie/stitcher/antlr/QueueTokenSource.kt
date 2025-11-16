package dev.kikugie.stitcher.antlr

import dev.kikugie.commons.collections.FixedQueue
import dev.kikugie.commons.then
import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenFactory
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenFactory
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair
import java.util.LinkedList
import java.util.Queue

internal abstract class QueueTokenSource(private val source: CharStream) : TokenSource {
    private val tokenQueue: Queue<AntlrToken> = LinkedList()
    protected val tokenSource: Pair<TokenSource, CharStream> = Pair(this, source)
    private var tokenFactory: TokenFactory<*> = CommonTokenFactory()

    protected abstract fun advance()
    protected fun push(token: AntlrToken): Boolean = tokenQueue.add(token)

    override fun nextToken(): Token = when (tokenQueue.size) {
        0 -> advance() then tokenQueue.element()
        1 -> advance() then tokenQueue.remove() then tokenQueue.element()
        else -> tokenQueue.remove() then tokenQueue.element()
    }

    override fun getLine(): Int = tokenQueue.element().line
    override fun getCharPositionInLine(): Int = tokenQueue.element().line
    override fun getInputStream(): CharStream = source
    override fun getSourceName(): String = source.sourceName
    override fun getTokenFactory(): TokenFactory<*> = tokenFactory
    override fun setTokenFactory(factory: TokenFactory<*>) {
        tokenFactory = factory
    }
}