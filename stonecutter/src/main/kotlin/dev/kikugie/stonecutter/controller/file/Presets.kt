package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stitcher.antlr.scanner.HashStyleScanner
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.transform.impl.LineCommentStrategy
import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.impl.StarCommentStrategy
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import dev.kikugie.stonecutter.controller.file.ScannerBuilder.Template

@StonecutterExperimentalFilesAPI
public sealed interface Presets {
    public object Scanner {
        /**
         * Matches `//` and `/* */`-style comments, with non-nested multiline comments.
         */
        @JvmField public val DoubleSlashStar: Template = Template(
            ::SlashStyleScanner,
            setOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
            setOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END),
        )

        /**
         * Matches `//` and `/* */`-style comments, with nested multiline comments.
         * Comments inside string templates are **not** recognized.
         */
        @JvmField public val DoubleSlashStarNested: Template = Template(
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
    }

    public object Commenter {
        /**
         * Wraps the text in `/* */`-style comments, replacing nested cases with `/^ ^/` placeholders.
         */
        @JvmField public val SlashStarFlat: CommentingStrategy = StarCommentStrategy(true)

        /**
         * Wraps the text in `/* */`-style comments, but doesn't insert `/^ ^/` placeholders like [SlashStarFlat]
         * because Kotlin handles comment depth.
         */
        @JvmField public val SlashStarNested: CommentingStrategy = StarCommentStrategy(false)

        @JvmField public val Hash: CommentingStrategy = lines("#")
        @JvmField public val DoubleSlash: CommentingStrategy = lines("//")

        @JvmStatic public fun lines(prefix: String): CommentingStrategy = LineCommentStrategy(prefix)
    }

    public object Uncommenter {
        /**
         * Removes single- and multi-line comments, handling the `/^ ^/` placeholders produced by [Commenter.JavaMultiline].
         */
        @JvmField public val DoubleSlashStar: UncommentingStrategy = StarCommentStrategy(true)

        /**
         * Removes single- and multi-line comments, inserting line breaks for the former ones.
         */
        @JvmField public val Basic: UncommentingStrategy = UncommentingStrategy { it, _, cl ->
            if ('\r' in cl || '\n' in cl) it + cl else it
        }
    }

    public object Swapper {
        /**
         * Replaces the content with the swap value, padding it with the content's indentation.
         */
        @JvmField public val Default: SwappingStrategy = StandardSwapStrategy
    }
}