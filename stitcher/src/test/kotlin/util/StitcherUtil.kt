package util

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.antlr.scanner.HashStyleScanner
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.issue.ProblemCause
import dev.kikugie.stitcher.issue.ProblemConsumer
import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.parser.adapter.ScannerAdapter
import dev.kikugie.stitcher.process
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.impl.LineCommentStrategy
import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.impl.StarCommentStrategy
import dev.kikugie.stitcher.transform.replacement.ReplacementBuilder
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import io.kotest.core.TestConfiguration
import io.kotest.engine.spec.tempfile
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

val PROBLEM_REPORTER = ProblemConsumer { file: Path, location: ProblemLocation, problem: ProblemCause ->
    println(buildString {
        append("e: ${formatLocation(file, location)} ${problem.message}")
        if (problem.exception != null) append("\nCaused by: ${problem.exception.stackTraceToString()}")
    })
}

val IMMEDIATE_THROW = ProblemConsumer { file: Path, location: ProblemLocation, problem: ProblemCause ->
    throw AssertionError(problem.message).apply {
        if (problem.exception != null) initCause(problem.exception)
    }
}

private fun formatLocation(file: Path, location: ProblemLocation): String = buildString {
    append("file://${file.absolutePathString()}")
    if (location.line >= 1) {
        append(":${location.line}")
        if (location.column >= 1)
            append(":${location.column}")
    }
}

inline fun TestConfiguration.process(contents: String, parameters: TransformParametersBuilder.() -> Unit = {}): String {
    val file = tempfile().toPath()
    file.writeText(contents, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

    val parameters = TransformParametersBuilder().apply(parameters).build()
    return process(file, contents, parameters, IMMEDIATE_THROW)
}

class TransformParametersBuilder {
    var adapter: ScannerAdapter.Factory = Scanner.Java
    var commenter: CommentingStrategy = Commenter.JavaStar
    var uncommenter: UncommentingStrategy = Uncommenter.Java
    var swapper: SwappingStrategy = StandardSwapStrategy

    val swaps: MutableMap<String, String> = mutableMapOf()
    val constants: MutableMap<String, Boolean> = mutableMapOf("true" to true, "false" to false)
    val dependencies: MutableMap<String, Version> = mutableMapOf()
    val replacements: ReplacementBuilder<*> = ReplacementBuilder()

    fun build() = TransformParameters(adapter, commenter, uncommenter, swapper, swaps, constants, dependencies, replacements.build())

    object Scanner {
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
        @JvmField public val Hash: Template = Template(
            ::HashStyleScanner,
            setOf(HashStyleScanner.HASH_COMMENT_START),
            setOf(HashStyleScanner.HASH_COMMENT_END)
        )

        data class Template(
            val constructor: (CharStream) -> Lexer,
            val openers: Set<Int>,
            val closers: Set<Int>
        ) : ScannerAdapter.Factory {
            override fun create(input: CharStream, problems: ProblemSource): ScannerAdapter =
                ScannerAdapter(constructor(input), openers.toIntArray(), closers.toIntArray(), problems)
        }
    }

    object Commenter {
        /**
         * Wraps the text in `/* */`-style comments, replacing nested cases with `/^ ^/` placeholders.
         */
        @JvmField val JavaStar: CommentingStrategy = StarCommentStrategy(true)

        /**
         * Wraps the text in `/* */`-style comments, but doesn't insert `/^ ^/` placeholders like [SlashStarFlat]
         * because Kotlin handles comment depth.
         */
        @JvmField val KotlinStar: CommentingStrategy = StarCommentStrategy(false)

        @JvmField val Hash: CommentingStrategy = lines("#")
        @JvmField val DoubleSlash: CommentingStrategy = lines("//")

        @JvmStatic fun lines(prefix: String): CommentingStrategy = LineCommentStrategy(prefix)
    }

    object Uncommenter {
        /**
         * Removes single- and multi-line comments, handling the `/^ ^/` placeholders produced by [Commenter.JavaMultiline].
         */
        @JvmField val Java: UncommentingStrategy = StarCommentStrategy(true)

        /**
         * Removes single- and multi-line comments, inserting line breaks for the former ones.
         */
        @JvmField val Basic: UncommentingStrategy = UncommentingStrategy { it, _, cl ->
            if ('\r' in cl || '\n' in cl) it + cl else it
        }
    }
}