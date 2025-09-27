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

@StonecutterExperimentalFilesAPI @FileHandlerDsl
public abstract class FileHandlerBuilder @Inject constructor(private val objects: ObjectFactory) : Named {
    @get:Nested public abstract val scanner: Property<ScannerBuilder>
    @get:Input public abstract val commenter: Property<CommentingStrategy>
    @get:Input public abstract val uncommenter: Property<UncommentingStrategy>
    @get:Input public abstract val swapper: Property<SwappingStrategy>

    init {
        swapper.convention(Swapper.Standard)
    }

    public fun swap(strategy: SwappingStrategy): Unit = swapper.set(strategy)
    public fun comment(strategy: CommentingStrategy): Unit = commenter.set(strategy)
    public fun uncomment(strategy: UncommentingStrategy): Unit = uncommenter.set(strategy)

    public fun scanner(action: ScannerBuilder.() -> Unit) {
        val builder = scanner.orNull ?: objects.newInstance<ScannerBuilder>()
        scanner.set(builder.apply(action))
    }

    public object Commenter {
        @JvmField public val JavaMultiline: CommentingStrategy = StarCommentStrategy(true)
        @JvmField public val KotlinMultiline: CommentingStrategy = StarCommentStrategy(false)
    }

    public object Uncommenter {
        @JvmField public val JavaLike: UncommentingStrategy = StarCommentStrategy(true)
    }

    public object Swapper {
        @JvmField public val Standard: SwappingStrategy = StandardSwapStrategy
    }
}