package dev.kikugie.stonecutter.build.task

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.process.SCPrepareTask
import dev.kikugie.stonecutter.TaskProviderMap
import dev.kikugie.stonecutter.build.StonecutterBuildImpl
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.create
import java.io.File

/**
 * Provides structured access to tasks created by [StonecutterBuild][dev.kikugie.stonecutter.build.StonecutterBuildExtension].
 */
@StonecutterAPI
public interface StonecutterBuildTasks {
    /**
     * Comment processing tasks for each source set in the project.
     * @see SCPrepareTask
     */
    public val prepare: TaskProviderMap<Identifier, SCPrepareTask>

    /**
     * Versioned source generating tasks for each source set in the project.
     */
    public val generate: TaskProviderMap<Identifier, Sync>

    /**Version switch merging tasks for each source set in the project.*/
    public val merge: TaskProviderMap<Identifier, Copy>

    /**`versions/**/build/stonecutter-cache/sources/`*/
    public val processedCacheDir: File

    /**`versions/**/build/generated/stonecutter/`*/
    public val generatedSourcesDir: File

    public fun prepareTaskName(src: SourceSet): String = "stonecutterPrepare${taskSuffix(src)}"

    public fun generateTaskName(src: SourceSet): String = "stonecutterGenerate${taskSuffix(src)}"

    public fun mergeTaskName(src: SourceSet): String = "stonecutterMerge${taskSuffix(src)}"

    /**
     * @return Empty string if [src] is `main`, otherwise its capitalised name
     */
    public fun taskSuffix(src: SourceSet): String =
        if (SourceSet.isMain(src)) "" else src.name.replaceFirstChar(Char::uppercase)

    public fun configureSource(src: SourceSet)

    @StonecutterInternalAPI
    public companion object {
        internal fun ExtensionContainer.tasksContainer(name: String, ext: StonecutterBuildImpl): StonecutterBuildTasks =
            create(StonecutterBuildTasks::class, name, StonecutterBuildTasksImpl::class, ext)
    }
}