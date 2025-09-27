package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stitcher.antlr.scanner.HashStyleScanner
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.tool.LexerGrammar
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import java.io.Serializable
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ScannerDsl

@StonecutterExperimentalFilesAPI @ScannerDsl
public interface ScannerBuilder {
    @get:Input public val constructor: Property<LexerConstructor>
    @get:Input public val openers: SetProperty<Int>
    @get:Input public val closers: SetProperty<Int>

    public fun lexer(constructor: LexerConstructor): Unit = this.constructor.set(constructor)
    public fun lexer(grammar: String): Unit = lexer(InterpretedLexerConstructor(grammar))
    public fun lexer(clazz: Class<out Lexer>): Unit = lexer(ReflectionLexerConstructor(clazz))
    public fun lexer(clazz: KClass<out Lexer>): Unit = lexer(clazz.java)

    public infix fun from(template: Template) {
        constructor.set(template.constructor)
        openers.set(template.openers)
        closers.set(template.closers)
    }

    public fun interface LexerConstructor : Serializable {
        public fun create(input: CharStream): Lexer
    }

    private class InterpretedLexerConstructor(str: String) : LexerConstructor {
        val grammar: LexerGrammar by lazy { LexerGrammar(str) }

        override fun create(input: CharStream): Lexer =
            grammar.createLexerInterpreter(input)
    }

    private class ReflectionLexerConstructor(cls: Class<out Lexer>) : LexerConstructor {
        val constructor: Constructor<out Lexer> by lazy { cls.getConstructor(CharStream::class.java) }

        override fun create(input: CharStream): Lexer =
            constructor.newInstance(input)
    }

    public data class Template(
        public val constructor: LexerConstructor,
        public val openers: Set<Int>,
        public val closers: Set<Int>
    )

    public companion object {
        @JvmField public val Java: Template = Template(
            ::SlashStyleScanner,
            setOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
            setOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END),
        )

        @JvmField public val Kotlin: Template = Template(
            { SlashStyleScanner(it).apply { nestMultiLineComments = true } },
            setOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
            setOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END),
        )

        @JvmField public val Properties: Template = Template(
            ::HashStyleScanner,
            setOf(HashStyleScanner.HASH_COMMENT_START),
            setOf(HashStyleScanner.HASH_COMMENT_END)
        )
    }
}