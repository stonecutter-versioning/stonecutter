package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.impl.StarCommentStrategy
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class FileHandlerDsl

/**
 * Provides a strategy for processing a file format.
 */
@StonecutterExperimentalFilesAPI @FileHandlerDsl
public abstract class FileHandlerBuilder @Inject constructor(objects: ObjectFactory) : Named {
    /**
     * Provides a strategy for parsing file comments.
     *
     * @see ScannerBuilder
     */
    @get:Nested public abstract val scanner: Property<ScannerBuilder>

    /**
     * Provides a strategy for adding comments to blocks of file content.
     *
     * The [CommentingStrategy] object receives a [String] corresponding to the block that needs to be commented out.
     * The main responsibilities of the strategy are:
     * - Maintain the formatting and indentation of the original content:
     * ```java
     * void incorrect() {
     *     //? if false
     * //    System.out.println("hey");
     * }
     *
     * void correct() {
     *     //? if false
     *     //System.out.println("hey");
     * }
     * ```
     * - Handle nested comments:
     * ```java
     * void incorrect() {
     *     //? if false {
     *     /*/*-- HEY --*/
     *     System.out.println("hey");
     *     *///?}
     * }
     *
     * void correct() {
     *     //? if false {
     *     /*/^-- HEY --^/
     *     System.out.println("hey");
     *     *///?}
     * }
     * ```
     *
     * *Both of these are handled by [Commenter.JavaMultiline], but take notes for your own implementations.*
     *
     * @see Commenter.JavaMultiline
     * @see Commenter.KotlinMultiline
     */
    @get:Input public abstract val commenter: Property<CommentingStrategy>

    /**
     * Provides a strategy for handling comment removal.
     *
     * The [UncommentingStrategy] receives the comment body, opener and closer as separate arguments.
     * The default value ([Uncommenter.Basic]) handles line break insertion for single-line comments,
     * and should be suitable for most cases.
     *
     * A custom implementation may be necessary if handling of [commenter] transformations is needed.
     * For example, [Uncommenter.JavaLike] handles converting `/^ ^/` placeholders to `/* */` comments
     * produced by [Commenter.JavaMultiline].
     */
    @get:Input public abstract val uncommenter: Property<UncommentingStrategy>

    /**
     * Provides a strategy for handling swap insertions.
     *
     * The [SwappingStrategy] receives the existing content and replacement value.
     * The main implementation responsibility is to maintain the content formatting and indentation,
     * which can be handled by the default value ([Swapper.Standard]).
     */
    @get:Input public abstract val swapper: Property<SwappingStrategy>

    init {
        scanner.set(objects.newInstance<ScannerBuilder>())
        swapper.convention(Swapper.Standard)
        uncommenter.convention(Uncommenter.Basic)
    }

    /**
     * Provides a strategy for handling swap insertions.
     *
     * The [SwappingStrategy] receives the existing content and replacement value.
     * The main implementation responsibility is to maintain the content formatting and indentation,
     * which can be handled by the default value ([Swapper.Standard]).
     */
    public fun swap(strategy: SwappingStrategy): Unit = swapper.set(strategy)

    /**
     * Provides a strategy for adding comments to blocks of file content.
     *
     * The [CommentingStrategy] object receives a [String] corresponding to the block that needs to be commented out.
     * The main responsibilities of the strategy are:
     * - Maintain the formatting and indentation of the original content:
     * ```java
     * void incorrect() {
     *     //? if false
     * //    System.out.println("hey");
     * }
     *
     * void correct() {
     *     //? if false
     *     //System.out.println("hey");
     * }
     * ```
     * - Handle nested comments:
     * ```java
     * void incorrect() {
     *     //? if false {
     *     /*/*-- HEY --*/
     *     System.out.println("hey");
     *     *///?}
     * }
     *
     * void correct() {
     *     //? if false {
     *     /*/^-- HEY --^/
     *     System.out.println("hey");
     *     *///?}
     * }
     * ```
     *
     * *Both of these are handled by [Commenter.JavaMultiline], but take notes for your own implementations.*
     *
     * @see Commenter.JavaMultiline
     * @see Commenter.KotlinMultiline
     */
    public fun comment(strategy: CommentingStrategy): Unit = commenter.set(strategy)

    /**
     * Provides a strategy for handling comment removal.
     *
     * The [UncommentingStrategy] receives the comment body, opener and closer as separate arguments.
     * The default value ([Uncommenter.Basic]) handles line break insertion for single-line comments,
     * and should be suitable for most cases.
     *
     * A custom implementation may be necessary if handling of [commenter] transformations is needed.
     * For example, [Uncommenter.JavaLike] handles converting `/^ ^/` placeholders to `/* */` comments
     * produced by [Commenter.JavaMultiline].
     */
    public fun uncomment(strategy: UncommentingStrategy): Unit = uncommenter.set(strategy)

    /**
     * Provides a strategy for parsing file comments.
     *
     * @see ScannerBuilder
     */
    public fun scanner(action: ScannerBuilder.() -> Unit): Unit = scanner.get().action()

    public object Commenter {
        /**
         * Wraps the text in `/* */`-style comments, replacing nested cases with `/^ ^/` placeholders.
         */
        @JvmField public val JavaMultiline: CommentingStrategy = StarCommentStrategy(true)

        /**
         * Wraps the text in `/* */`-style comments, but doesn't insert `/^ ^/` placeholders like [JavaMultiline]
         * because Kotlin handles comment depth.
         */
        @JvmField public val KotlinMultiline: CommentingStrategy = StarCommentStrategy(false)
    }

    public object Uncommenter {
        /**
         * Removes single- and multi-line comments, handling the `/^ ^/` placeholders produced by [Commenter.JavaMultiline].
         */
        @JvmField public val JavaLike: UncommentingStrategy = StarCommentStrategy(true)

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
        @JvmField public val Standard: SwappingStrategy = StandardSwapStrategy
    }
}