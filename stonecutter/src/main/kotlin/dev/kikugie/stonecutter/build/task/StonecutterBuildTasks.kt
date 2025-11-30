package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.data.whatever.TaskProviderMap
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class BuildTasksDsl

/**Extension interface providing access to Stonecutter file processing tasks.*/
@BuildTasksDsl
public interface StonecutterBuildTasks {
    /**
     * Per-[SourceSet] file processing tasks.
     *
     * Processes comments in files and saves changed ones to the [processedCacheDir].
     */
    public val prepare: TaskProviderMap<StonecutterPrepareTask>

    /**
     * Per-[SourceSet] code generation tasks.
     *
     * Merges modified files produced by the [prepare] task with
     * versioned overrides and the shared source to the [generatedSourcesDir].
     */
    public val generate: TaskProviderMap<Sync>

    /**
     * Per-[SourceSet] version switching tasks.
     *
     * Copies processed [generatedSourcesDir] into the shared `src/`.
     */
    public val merge: TaskProviderMap<Copy>

    /**`build/stonecutter-cache/`*/
    public val processedCacheDir: DirectoryProperty

    /**`build/generated/stonecutter/`*/
    public val generatedSourcesDir: DirectoryProperty

    /**Adds the generated directories to the provided [src].*/
    public fun configureSource(src: SourceSet)
}