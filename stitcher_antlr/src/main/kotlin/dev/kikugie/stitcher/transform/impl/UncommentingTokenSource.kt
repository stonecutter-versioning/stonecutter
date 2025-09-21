package dev.kikugie.stitcher.transform.impl

import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.parse.adapter.InlineTokenStream
import dev.kikugie.stitcher.parse.builder.LayoutBuilder
import dev.kikugie.stitcher.transform.RuntimeParameters
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.util.asSequence
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenFactory
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenFactory
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair

internal class UncommentingTokenSource(
    val runtime: RuntimeParameters,
    val params: TransformParameters,
    val blocks: List<BlockToken>
) : TokenSource {
    private val src = Pair(this as TokenSource, runtime.input)
    private val tokens = sequence {
        for (it in blocks) unwrap(it)
        yield(factory.create(Token.EOF, "\u0000"))
    }.iterator()
    private var factory: TokenFactory<*> = CommonTokenFactory()
    private lateinit var token: Token

    override fun nextToken(): Token = tokens.next().also { token = it }
    override fun getLine(): Int = if (::token.isInitialized) token.line else 1
    override fun getCharPositionInLine(): Int = if (::token.isInitialized) token.charPositionInLine else 0
    override fun getInputStream(): CharStream = runtime.input
    override fun getSourceName(): String = runtime.input.sourceName
    override fun getTokenFactory(): TokenFactory<*> = factory
    override fun setTokenFactory(factory: TokenFactory<*>) {
        this.factory = factory
    }

    private suspend fun SequenceScope<Token>.unwrap(block: BlockToken): Unit = when(block) {
        // TODO: Store line info
        is BlockToken.Content -> {
            val range = block.leaf.range
            val token = factory.create(src, LayoutBuilder.CONTENT, null, Token.DEFAULT_CHANNEL, range.first, range.last, 1, 0)
            yield(token)
        }
        is BlockToken.Comment -> {
            val content = params.uncommenter.uncomment(block.body.text)
            val lexer = params.adapter.create(content.toStream(), runtime.sink)
            val stream = InlineTokenStream(CommonTokenStream(lexer), CommonTokenFactory(), block.body.range.first)
            yieldAll(stream.asSequence())
        }
        else -> error("Unexpected block type: ${block::class.simpleName}")
    }
}