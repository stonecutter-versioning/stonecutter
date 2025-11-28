package dev.kikugie.stonecutter.controller.file

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.tool.LexerGrammar
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.Serializable
import java.lang.reflect.Constructor
import javax.inject.Inject
import kotlin.reflect.KClass

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ScannerDsl

@ScannerDsl
public abstract class ScannerBuilder @Inject constructor(objects: ObjectFactory) : Serializable {
    public abstract val openers: SetProperty<Int>
    public abstract val closers: SetProperty<Int>
    public abstract val lexer: Property<LexerFactory>

    public fun openers(vararg types: Int): Unit = openers(types.asIterable())
    public fun openers(types: Iterable<Int>) {
        openers.set(types.toSet())
    }

    public fun closers(vararg types: Int): Unit = closers(types.asIterable())
    public fun closers(types: Iterable<Int>) {
        closers.set(types.toSet())
    }

    public fun interpreted(grammar: CharSequence): LexerFactory = InterpretedLexerFactory(grammar.toString())
    public fun reflected(cls: Class<out Lexer>): LexerFactory = ReflectiveLexerFactory(cls)
    public fun reflected(cls: KClass<out Lexer>): LexerFactory = reflected(cls.java)
    public inline fun <reified T : Lexer> reflected(): LexerFactory = reflected(T::class.java)

    public fun interface LexerFactory {
        public fun create(input: CharStream): Lexer
    }
}

private class InterpretedLexerFactory(grammarString: String) : ScannerBuilder.LexerFactory {
    val grammar: LexerGrammar by lazy { LexerGrammar(grammarString) }
    override fun create(input: CharStream): Lexer = grammar.createLexerInterpreter(input)
}

private class ReflectiveLexerFactory(lexerClass: Class<out Lexer>) : ScannerBuilder.LexerFactory {
    val constructor: Constructor<out Lexer> by lazy { lexerClass.getConstructor(CharStream::class.java) }
    override fun create(input: CharStream): Lexer = constructor.newInstance(input)
}