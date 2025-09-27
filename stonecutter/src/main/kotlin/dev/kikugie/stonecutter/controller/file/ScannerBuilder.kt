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

/**
 * Provides a strategy for parsing comments in a file.
 *
 * ### Parsing workflow
 * As the first parsing step, Stonecutter needs to differentiate what is a comment in a file.
 * This is represented as a token stream in the following sequence:
 * ```
 * <Content> <Comment-Start> <Comment-Body> <Comment-End> ...
 * ```
 * Since the token stream is monotone - after `<Content>` always comes `<Comment-Start>`,
 * and after `<Comment-Body>` always comes `<Comment-End>`, the lexer can only provide
 * comment opening and closing tokens, leaving the [ScannerAdapter][dev.kikugie.stitcher.parse.adapter.ScannerAdapter]
 * to fill in the rest.
 *
 * ### Lexer registration
 * The lexer should be registered using one of the [lexer] functions,
 * with the comment opening and closing token types added to [openers] and [closers] respectively.
 * The lexer **must** only emit the specified tokens, as any other will result in an error.
 * It is recommended to `-> skip` irrelevant characters instead of sending them to `-> channel(HIDDEN)`,
 * as they are never used by the adapter, but can cause performance issues due to the token instantiation.
 *
 * @see <a href="https://codeberg.org/stonecutter/stonecutter/src/branch/0.8/stitcher/src/main/antlr/dev/kikugie/stitcher/antlr/scanner/SlashStyleScanner.g4>`//` and `/* */`-comment scanner</a>
 * @see <a href="https://codeberg.org/stonecutter/stonecutter/src/branch/0.8/stitcher/src/main/antlr/dev/kikugie/stitcher/antlr/scanner/HashStyleScanner.g4">`#`-comment scanner</a>
 */
@StonecutterExperimentalFilesAPI @ScannerDsl
public interface ScannerBuilder {
    /**The lexer factory for the configured file format.*/
    @get:Input public val constructor: Property<LexerConstructor>

    /**The comment opener token types.*/
    @get:Input public val openers: SetProperty<Int>

    /**The comment closer token types.*/
    @get:Input public val closers: SetProperty<Int>

    /**
     * Registers a comment lexer using a dynamic [constructor].
     *
     * The provided [LexerConstructor] implementation **must** be stateless.
     */
    public fun lexer(constructor: LexerConstructor): Unit = this.constructor.set(constructor)

    /**
     * Registers a comment lexer based on the provided [grammar] string.
     */
    public fun lexer(grammar: String): Unit = lexer(InterpretedLexerConstructor(grammar))

    /**
     * Registers a comment lexer based on its [class][clazz].
     *
     * The provided class **must** have a public single-argument constructor accepting a [CharStream] object.
     */
    public fun lexer(clazz: Class<out Lexer>): Unit = lexer(ReflectionLexerConstructor(clazz))

    /**
     * Registers a comment lexer based on its [class][clazz].
     *
     * The provided class **must** have a public single-argument constructor accepting a [CharStream] object.
     */
    public fun lexer(clazz: KClass<out Lexer>): Unit = lexer(clazz.java)

    /**
     * Applies configuration of the given [template].
     *
     * @see [ScannerBuilder.Companion]
     */
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
        /**
         * Matches `//` and `/* */`-style comments, with non-nested multiline comments.
         */
        @JvmField public val Java: Template = Template(
            ::SlashStyleScanner,
            setOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
            setOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END),
        )

        /**
         * Matches `//` and `/* */`-style comments, with nested multiline comments.
         * Comments inside string templates are **not** recognized.
         */
        @JvmField public val Kotlin: Template = Template(
            { SlashStyleScanner(it).apply { nestMultiLineComments = true } },
            setOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
            setOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END),
        )

        /**
         * Matches `#`-style comments.
         */
        @JvmField public val Properties: Template = Template(
            ::HashStyleScanner,
            setOf(HashStyleScanner.HASH_COMMENT_START),
            setOf(HashStyleScanner.HASH_COMMENT_END)
        )
    }
}