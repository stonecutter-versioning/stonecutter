package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.data.whatever.TaskProviderMap
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class BuildTasksDsl

@BuildTasksDsl
public interface StonecutterBuildTasks {
    public val prepare: TaskProviderMap<StonecutterPrepareTask>
    public val generate: TaskProviderMap<Sync>
    public val merge: TaskProviderMap<Copy>

    public val processedCacheDir: DirectoryProperty
    public val generatedSourcesDir: DirectoryProperty

    public fun configureSource(src: SourceSet)
}