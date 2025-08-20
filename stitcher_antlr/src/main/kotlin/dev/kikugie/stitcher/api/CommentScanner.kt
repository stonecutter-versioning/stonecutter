package dev.kikugie.stitcher.api

import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.tool.LexerGrammar
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

internal inline fun scanner(action: ScannerBuilder.() -> Unit): ScannerAdapter.Factory =
    ScannerBuilder().apply(action).build()

@DslMarker
internal annotation class ScannerDSL

// TODO: add associated file types?
@ScannerDSL
public class ScannerBuilder @PublishedApi internal constructor() {
    public val openingTokens: MutableSet<Int> = mutableSetOf()
    public val closingTokens: MutableSet<Int> = mutableSetOf()
    private lateinit var lexerConstructor: LexerFactory

    public fun lexer(cls: KClass<out Lexer>): Unit = lexer(cls.java)
    public fun lexer(cls: Class<out Lexer>): Unit = lexer(ReflectionLexerFactory(cls))
    public fun lexer(grammar: String): Unit = lexer(InterpreterLexerFactory(grammar))
    public fun lexer(factory: LexerFactory) {
        lexerConstructor = factory
    }

    @PublishedApi
    internal fun build(): ScannerAdapter.Factory {
        val factory = lexerConstructor
        val openers = openingTokens.ifEmpty { error("No opening tokens defined") }.toIntArray()
        val closers = closingTokens.ifEmpty { error("No closing tokens defined") }.toIntArray()
        return ScannerAdapter.Factory { stream, collector ->
            ScannerAdapter(factory.create(stream), openers, closers, collector)
        }
    }
}

public fun interface LexerFactory {
    public fun create(input: CharStream): Lexer
}

private class ReflectionLexerFactory(cls: Class<out Lexer>) : LexerFactory {
    private val constructor: Constructor<out Lexer> by lazy { cls.getConstructor(CharStream::class.java) }
    override fun create(input: CharStream): Lexer = constructor.newInstance(input)
}

private class InterpreterLexerFactory(grammar: String) : LexerFactory {
    val grammar by lazy { LexerGrammar(grammar) }
    override fun create(input: CharStream): Lexer = grammar.createLexerInterpreter(input)
}