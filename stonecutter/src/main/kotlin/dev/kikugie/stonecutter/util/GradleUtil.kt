package dev.kikugie.stonecutter.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.DefaultTaskExecutionRequest
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import org.gradle.work.InputChanges
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import kotlin.io.deleteRecursively

internal val Project.sourceSets: SourceSetContainer
    get() = project.the<SourceSetContainer>()

internal inline fun <reified T : Any> ObjectFactory.newInstance(vararg parameters: Any, build: T.() -> Unit) =
    newInstance<T>(*parameters).apply(build)

internal inline fun <reified T : Any> ObjectFactory.newInstance(build: Action<T>, vararg parameters: Any) =
    newInstance<T>(*parameters).apply(build::execute)

internal fun SourceSet.allSources() = sequence {
    yield(java)
    yield(resources)
    extensions.extensionsSchema.asSequence()
        .mapNotNull { extensions[it.name] }
        .filterIsInstance<SourceDirectorySet>()
        .let { yieldAll(it) }
}

internal fun InputChanges.clearIfNotIncremental(vararg files: File) {
    if (isIncremental) return
    for (it in files) if (it.exists()) {
        it.deleteRecursively()
        it.mkdirs()
    }
}

internal val isIdeaSync: Boolean get() = System.getProperty("idea.sync.active", "false").toBoolean()
internal fun Gradle.requestTasks(tasks: Iterable<String>, path: String, dir: File) = startParameter.run {
    setTaskRequests(taskRequests + DefaultTaskExecutionRequest(tasks, path, dir))
}

internal val Project.projectDirectory get() = layout.projectDirectory.asFile
internal val Project.buildDirectory get() = layout.buildDirectory.asFile.get()

internal fun <T : Any> Property<T>.set(factory: ProviderFactory, provider: () -> T) {
    set(factory.provider(provider))
}

internal inline fun WorkerExecutor.execute(action: (queue: WorkQueue) -> Unit) =
    noIsolation().apply(action).await()