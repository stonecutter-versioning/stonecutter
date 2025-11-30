package dev.kikugie.stonecutter.controller.file

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.tool.LexerGrammar
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.intellij.lang.annotations.Language
import java.io.Serializable
import java.lang.reflect.Constructor
import javax.inject.Inject
import kotlin.reflect.KClass

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ScannerDsl

/**
 * Extension interface for configuring a comment lexer for the associated file type.
 *
 * The comment lexer *(AKA scanner)* should only emit tokens declared in [openers]
 * and [closers] sets. The complete requirements can be found on the
 * [wiki](https://stonecutter.kikugie.dev/wiki/config/handlers#scanner).
 *
 * @see FileHandlerContainer
 */
@ScannerDsl
public abstract class ScannerBuilder @Inject constructor(objects: ObjectFactory) {
    /**ANTLR token types representing comment openers.*/
    public abstract val openers: SetProperty<Int>

    /**ANTLR token types representing comment closers.*/
    public abstract val closers: SetProperty<Int>

    /**The comment lexer constructor.*/
    public abstract val lexer: Property<LexerFactory>

    /**Copies all configurations of the [other] builder.*/
    public fun copy(other: ScannerBuilder) {
        openers(other.openers.get())
        closers(other.closers.get())
        lexer.set(other.lexer.get())
    }

    /**Overwrites the [openers] with the provided [types].*/
    public fun openers(vararg types: Int): Unit = openers(types.asIterable())

    /**Overwrites the [openers] with the provided [types].*/
    public fun openers(types: Iterable<Int>) {
        openers.set(types.toSet())
    }

    /**Overwrites the [closers] with the provided [types].*/
    public fun closers(vararg types: Int): Unit = closers(types.asIterable())

    /**Overwrites the [closers] with the provided [types].*/
    public fun closers(types: Iterable<Int>) {
        closers.set(types.toSet())
    }

    /**Creates a new lexer based on the ANTLR [grammar].*/
    public fun interpreted(@Language("ANTLRv4") grammar: CharSequence): LexerFactory = InterpretedLexerFactory(grammar.toString())

    /**Creates a new lexer by instantiating the provided [cls].*/
    public fun reflected(cls: Class<out Lexer>): LexerFactory = ReflectiveLexerFactory(cls)

    /**Creates a new lexer by instantiating the provided [cls].*/
    public fun reflected(cls: KClass<out Lexer>): LexerFactory = reflected(cls.java)

    /**Creates a new lexer by instantiating the provided [T] class.*/
    public inline fun <reified T : Lexer> reflected(): LexerFactory = reflected(T::class.java)

    /**
     * A [lexer] instantiator.
     *
     * The implementation **must not reference external parameters**,
     * as it will be serialized after the configuration stage.
     */
    public fun interface LexerFactory : Serializable {
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