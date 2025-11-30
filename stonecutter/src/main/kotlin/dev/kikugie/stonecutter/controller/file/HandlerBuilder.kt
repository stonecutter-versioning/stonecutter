package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stitcher.transform.impl.LineCommentStrategy
import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class FileHandlerDsl

/**
 * Extension class for configuring how Stonecutter should process the associated file format.
 * @see FileHandlerContainer
 */
@FileHandlerDsl
public abstract class HandlerBuilder @Inject constructor(objects: ObjectFactory) : Named {
    /**Comment lexer configuration. Not reassignable - configure instead.*/
    public abstract val scanner: Property<ScannerBuilder>

    /**
     * Text commenting function.
     *
     * The function receives a text block and should return a new string,
     * commented according to the associated file format.
     */
    public abstract val commenter: Property<CommentingStrategy>

    /**
     * Text uncommenting function.
     *
     * The function receives a comment body and should reformat it if needed.
     * *In most cases the default value works well enough*.
     */
    public abstract val uncommenter: Property<UncommentingStrategy>

    /**
     * Swap formatting function.
     *
     * The function receives a text block and the value that it should be replaced with,
     * and output a formatted string. *In most cases the default value works well enough*.
     */
    public abstract val swapper: Property<SwappingStrategy>

    init {
        swapper.convention(StandardSwapStrategy)
        uncommenter.convention(BASIC_UNCOMMENTER)
        scanner.value(objects.newInstance<ScannerBuilder>()).disallowChanges()
    }

    /**Copies all configurations of the [other] builder.*/
    public fun copy(other: HandlerBuilder) {
        scanner.get().copy(other.scanner.get())
        commenter.set(other.commenter.get())
        uncommenter.set(other.uncommenter.get())
        swapper.set(other.swapper.get())
    }

    /**Configures the [scanner] property.*/
    public fun scanner(action: Action<ScannerBuilder>) {
        action.execute(scanner.get())
    }

    /**Creates a [commenter] that prepends the given [prefix] on each line.*/
    public fun line(prefix: String): CommentingStrategy =
        LineCommentStrategy(prefix)

    private companion object {
        val BASIC_UNCOMMENTER: UncommentingStrategy = UncommentingStrategy { it, _, cl ->
            if ('\r' in cl || '\n' in cl) it + cl else it
        }
    }
}