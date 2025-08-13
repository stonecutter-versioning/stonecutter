package dev.kikugie.stitcher.api

import dev.kikugie.stitcher.antlr.adapter.ScannerAdapter
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.tool.LexerGrammar
import kotlin.reflect.KClass

inline fun scanner(action: ScannerBuilder.() -> Unit): ScannerAdapter.Factory =
    ScannerBuilder().apply(action).build()

@DslMarker
annotation class ScannerDSL

// TODO: add associated file types?
@ScannerDSL
class ScannerBuilder @PublishedApi internal constructor() {
    val openingTokens: MutableSet<Int> = mutableSetOf()
    val closingTokens: MutableSet<Int> = mutableSetOf()
    private lateinit var lexerConstructor: LexerFactory

    fun lexer(cls: KClass<out Lexer>): Unit = lexer(cls.java)
    fun lexer(cls: Class<out Lexer>): Unit = lexer(ReflectionLexerFactory(cls))
    fun lexer(grammar: String): Unit = lexer(InterpreterLexerFactory(grammar))
    fun lexer(factory: LexerFactory) {
        lexerConstructor = factory
    }

    @PublishedApi
    internal fun build(): ScannerAdapter.Factory {
        val factory = lexerConstructor
        val openers = openingTokens.ifEmpty { error("No opening tokens defined") }.toIntArray()
        val closers = closingTokens.ifEmpty { error("No closing tokens defined") }.toIntArray()
        return ScannerAdapter.Factory {
            ScannerAdapter(factory.create(it), openers, closers)
        }
    }
}

fun interface LexerFactory {
    fun create(input: CharStream): Lexer
}

private class ReflectionLexerFactory(cls: Class<out Lexer>) : LexerFactory {
    private val constructor by lazy { cls.getConstructor(CharStream::class.java) }
    override fun create(input: CharStream): Lexer = constructor.newInstance(input)
}

private class InterpreterLexerFactory(grammar: String) : LexerFactory {
    val grammar by lazy { LexerGrammar(grammar) }
    override fun create(input: CharStream): Lexer = grammar.createLexerInterpreter(input)
}