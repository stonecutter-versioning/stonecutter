package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import java.io.Serializable
import javax.inject.Inject

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class FileHandlerDsl

@FileHandlerDsl
public abstract class FileHandlerBuilder @Inject constructor(objects: ObjectFactory) : Named, Serializable {
    public abstract val scanner: Property<ScannerBuilder>
    public abstract val commenter: Property<CommentingStrategy>
    public abstract val uncommenter: Property<UncommentingStrategy>
    public abstract val swapper: Property<SwappingStrategy>

    init {
        swapper.convention(StandardSwapStrategy)
        uncommenter.convention(BASIC_UNCOMMENTER)
        scanner.value(objects.newInstance<ScannerBuilder>())
    }

    public fun scanner(action: Action<ScannerBuilder>) {
        action.execute(scanner.get())
    }

    private companion object {
        val BASIC_UNCOMMENTER: UncommentingStrategy = UncommentingStrategy { it, _, cl ->
            if ('\r' in cl || '\n' in cl) it + cl else it
        }
    }
}